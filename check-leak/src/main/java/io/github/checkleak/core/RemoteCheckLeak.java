/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.checkleak.core;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.sun.tools.attach.VirtualMachine;
import sun.tools.attach.HotSpotVirtualMachine;

public class RemoteCheckLeak implements Runnable {

   volatile ExecutorService executorService;
   private volatile File report;
   private long sleep = 60_000;
   volatile boolean active = true;

   public void stop() {
      active = false;
      if (executorService != null) {
         executorService.shutdown();
         executorService = null;
      }
   }

   public boolean isActive() {
      return active;
   }

   public RemoteCheckLeak setActive(boolean active) {
      this.active = active;
      return this;
   }

   public File getReport() {
      return report;
   }

   public RemoteCheckLeak setReport(File report) {
      this.report = report;
      executorService = Executors.newSingleThreadExecutor();
      try {
         TableGenerator.installStuff(report);
      } catch(Exception e) {
         e.printStackTrace();
      }
      return this;
   }

   public long getSleep() {
      return sleep;
   }

   public RemoteCheckLeak setSleep(long sleep) {
      this.sleep = sleep;
      return this;
   }

   private static long copy(InputStream in, OutputStream out) throws Exception {
      try {
         byte[] buffer = new byte[1024];
         int len = in.read(buffer);
         while (len != -1) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
         }
         return len;
      } finally {
         in.close();
      }
   }

   public static boolean printBanner(PrintStream out) {
      try {
         copy(Agent.class.getResourceAsStream("banner.txt"), out);
         return true;
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }

   public static class Parameters {
      long sleep = 30_000;
      String pid;
      String report;

      public void parse(String... arg) {

         try {
            for (int i = 0; i < arg.length; i++) {
               String argument = arg[i];
               if (argument.startsWith("-")) {
                  switch (argument) {
                     case "-sleep":
                     case "--sleep":
                        sleep = Long.parseLong(arg[i + 1]);
                        break;
                     case "-pid":
                     case "--pid":
                        pid = arg[i + 1];
                        break;
                     case "--report":
                        report = arg[i + 1];
                        break;
                     default:
                        printUsage("Invalid parameter " + argument);
                        System.exit(-1);
                  }
                  i++;
               }
            }
         } catch (Throwable e) {
            e.printStackTrace();
            printUsage(e.getMessage());
            System.exit(-1);
         }
      }
   }

   public static void printUsage(String message) {
      System.out.println(message);
      System.out.println("java -jar check-leak.jar -sleep timeout -pid <PID> -report <report-location>");
      System.out.println("java -jar check-leak.jar install <dll-output>");
   }

   public static void main(String arg[]) {
      printBanner(System.out);

      if (arg.length > 0 && arg[0].equals("install")) {
         try {
            Installer.install(new File(arg[1]));
         } catch (Throwable e) {
            printUsage(e.getMessage());
         }
         return;
      }

      Parameters parameters = new Parameters();
      parameters.parse(arg);

      if (parameters.pid == null) {
         printUsage("you must specify -pid <PID>");
         System.exit(-1);
      }

      try {
         RemoteCheckLeak remoteCheckLeak = new RemoteCheckLeak();
         if (parameters.report != null) {
            System.out.println("Setting report at " + parameters.report);
            remoteCheckLeak.setReport(new File(parameters.report));
         }
         remoteCheckLeak.connect(parameters.pid);
         remoteCheckLeak.setSleep(parameters.sleep);
         remoteCheckLeak.run();
      } catch (Throwable e) {
         e.printStackTrace();
      }
   }

   VirtualMachine machine;
   HotSpotVirtualMachine hotSpotVirtualMachine;

   HashMap<String, Histogram> maxValues = new HashMap<>();

   public void connect(String id) throws Exception {
      machine = VirtualMachine.attach("" + id);
      if (!(machine instanceof HotSpotVirtualMachine)) {
         throw new RuntimeException("Cannot connect to HotSpotVirtualMachine. Type is " + machine.getClass());
      }
      hotSpotVirtualMachine = (HotSpotVirtualMachine) machine;
   }

   public InputStream execute(String command) throws Exception {
      return hotSpotVirtualMachine.executeJCmd(command);
   }

    void processHistogram() throws Exception {

      // GC Histogram will not return an item for zeroed classes
      // say a class has released all of its objects,
      // it will not appear in the report.
      // So we take a snapshot here, remove the ones the appear to later on consider a zero object
      final Set<String> zeroNames = new HashSet<>();
      zeroNames.addAll(maxValues.keySet());
      long time = System.currentTimeMillis();
      getHistogram(time, (line, histogram) -> {
         zeroNames.remove(histogram.name);
         Histogram currentMaxValue = maxValues.get(histogram.name);

         if (currentMaxValue == null) {
            maxValues.put(histogram.name, histogram);
            try {
               histogram.onOver(true, histogram.copy(), report, executorService);
            } catch (Exception e) {
               e.printStackTrace();
            }
         } else {
            try {
               currentMaxValue.check(histogram, report, executorService);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });

      zeroNames.forEach(zero -> {
         Histogram zeroed = new Histogram(zero, 0l, 0l, time);
         Histogram currrentMaxValue = maxValues.get(zero);
         if (currrentMaxValue != null) {
            try {
               currrentMaxValue.check(zeroed, report, executorService);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });

   }

   public void getHistogram(long time, BiConsumer<String, Histogram> consumer) throws Exception {
      InputStream inputStream = execute("GC.class_histogram");
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      int lineNumber = 0;

      String line = null;
      while ((line = reader.readLine()) != null) {
         lineNumber++;
         if (lineNumber <= 2) {
            continue;
         }

         if (line.startsWith("Total")) {
            continue;
         }

         Histogram histogram = Histogram.parseLine(line, time);

         consumer.accept(line, histogram);
      }
   }

   public void disconnect() throws Exception {
      machine.detach();
   }

   @Override
   public void run() {
      try {
         while (active) {
            System.out.println("*******************************************************************************************************************************");
            if (report != null) {
               CountDownLatch latch = new CountDownLatch(1);
               ExecutorService service = executorService;
               if (service != null) {
                  service.execute(() -> {
                     try {
                        processHistogram();
                        generateIndex(report, maxValues.values());
                     } catch (Throwable e) {
                        e.printStackTrace();
                     } finally {
                        latch.countDown();
                     }
                  });
               }
               latch.await(1, TimeUnit.MINUTES);
            }
            Thread.sleep(sleep);
         }
      } catch (Throwable e) {
         e.printStackTrace();
      }
   }

   public static void generateIndex(File report, Collection<Histogram> values) throws Exception {
      FileOutputStream fileOutputStream = new FileOutputStream(new File(report, "index.html"));
      PrintStream out = new PrintStream(new BufferedOutputStream(fileOutputStream));

      out.println("<!DOCTYPE html>");
      out.println("<html>");

      TableGenerator.addScriptHeader(out, "mainTable");

      out.println("<body>");

      TableGenerator.tableBegin(out, "mainTable");
      TableGenerator.tableHeader(out, "bytes", "peak bytes", "instances", "peak instances", "peak increases", "className");

      ArrayList<Histogram> histogram = new ArrayList<>(values.size());
      histogram.addAll(values);

      Collections.sort(histogram, new CompareOver());

      histogram.forEach(h -> {
         TableGenerator.tableLine(out,  "" + h.getBytes(), "" + h.getMaxBytes(), "" + h.getInstances(), "" + h.getMaxInstances(), "" + h.getTimesOver(), "" + "<a href='" + h.getFileName() + "'>" + h.getName() + "</a>");
      });
      TableGenerator.tableFooter(out);

      out.println("</body>");
      out.close();

   }

   private static class CompareOver implements Comparator<Histogram> {

      @Override
      public int compare(Histogram o1, Histogram o2) {
         if (o2.timesOver > o1.timesOver) {
            return 1;
         } else if (o2.timesOver < o1.timesOver) {
            return -1;
         } else if (o2.bytes > o1.bytes) {
            return 1;
         } else if (o2.bytes < o1.bytes) {
            return -1;
         } else {
            return o1.name.compareTo(o2.name);
         }
      }
   }



}
