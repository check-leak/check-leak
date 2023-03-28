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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.sun.tools.attach.VirtualMachine;
import io.github.checkleak.core.util.DateOps;
import io.github.checkleak.core.util.TDumpAnalyzer;
import io.github.checkleak.core.util.TableGenerator;
import sun.tools.attach.HotSpotVirtualMachine;

import static io.github.checkleak.core.util.HTMLHelper.makeLink;

public class RemoteCheckLeak implements Runnable {

   PrintStream out = System.out;

   public void setOut(PrintStream out) {
      this.out = out;
   }

   volatile ExecutorService executorService;
   volatile Executor executor;
   private File report;
   private File logs;
   private long sleep = 60_000;
   private final CountDownLatch latchRunning = new CountDownLatch(1);
   volatile boolean active = true;

   // This is used to generate the logs view
   ArrayList<Long> processedTmes = new ArrayList<Long>();

   public void stop() {
      active = false;
      latchRunning.countDown();
      if (executorService != null) {
         executorService.shutdown();
         executorService = null;
      }
   }

   public boolean isActive() {
      return active;
   }

   public File getReport() {
      return report;
   }

   public RemoteCheckLeak setReport(File report) {
      this.report = report;
      this.logs = new File(report, "logs");
      this.logs.mkdirs();

      try {
         TableGenerator.installStuff(report);
         TDumpAnalyzer.installStuff(logs);
      } catch(Exception e) {
         e.printStackTrace();
      }

      if (executor == null) {
         startExecutor();
      }
      return this;
   }

   public Thread startThread() {
      Thread thread = new Thread(this);
      thread.start();
      return thread;
   }

   // You have to send a singleThreaded executor, or some equivalent that will guarantee a single thread executed every time
   public RemoteCheckLeak setExecutor(Executor executor) {
      this.executor = executor;
      return this;
   }

   public RemoteCheckLeak startExecutor() {
      executorService = Executors.newSingleThreadExecutor();
      executor = executorService;
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

            if (report != null && sleep < 1000) {
               printUsage("It is dangerous to use report with a very short sleep (less than 1 second). Reports may still being generated in the background while the next scan is being performed.");
               System.exit(-1);
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
         } else {
            remoteCheckLeak.startExecutor();
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
      processedTmes.add(time);
      File histogramFile;
      File threadDumpFile;
      File threadDumpAnalyzerFile;

      if (report == null) {
         histogramFile = threadDumpFile = threadDumpAnalyzerFile = null;
      } else {
         histogramFile = new File(logs, DateOps.getHistogramFileName(time));
         threadDumpFile = new File(logs, DateOps.getTDumpFileName(time));
         threadDumpAnalyzerFile = new File(logs, DateOps.getTDumpAnalyzerFileName(time));
      }

      try (PrintStream histogramOutputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(histogramFile)))) {
         getHistogram(time, histogramOutputStream, (line, histogram) -> {
            zeroNames.remove(histogram.name);
            Histogram currentMaxValue = maxValues.get(histogram.name);

            if (currentMaxValue == null) {
               maxValues.put(histogram.name, histogram);
               histogram.addHistory(histogram.copy());
               try {
                  histogram.onOver(true, histogram, report, executor, out);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            } else {
               try {
                  currentMaxValue.check(histogram, report, executor, out);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         });
      }

       if (report != null) {

         {
            String tdump = getThreadDump();
            TDumpAnalyzer.installDump(threadDumpFile, threadDumpAnalyzerFile, tdump);
         }
      }

      // the GC Histogram will simply ignore any class that now has 0 instances.
      // This is to filter the ones that disappeared so we can show them zeroed in the charts
      zeroNames.forEach(zero -> {
         Histogram zeroed = new Histogram(zero, 0l, 0l, time);
         Histogram currrentMaxValue = maxValues.get(zero);
         if (currrentMaxValue != null) {
            try {
               currrentMaxValue.check(zeroed, report, executor, out);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
   }

   private void execute(String command, Consumer<String> lineConsumer) throws Exception {
      try (InputStream inputStream = execute(command)) {
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
         String line = null;
         while ((line = reader.readLine()) != null) {
            lineConsumer.accept(line);
         }
      }
   }

   public String getThreadDump() throws Exception {
      StringBuffer buffer = new StringBuffer();
      execute("Thread.print", line -> buffer.append(line + "\n"));
      return buffer.toString();
   }

   public Map<String, Histogram> parseHistogram() throws Exception {
      HashMap<String, Histogram> histogramHashMap = new HashMap<>();

      getHistogram(System.currentTimeMillis(), null, (line, histogram) -> {
         histogramHashMap.put(histogram.name, histogram);
      });

      return histogramHashMap;
   }

   public void getHistogram(long time, PrintStream output, BiConsumer<String, Histogram> consumer) throws Exception {
      AtomicInteger lineNumber = new AtomicInteger(0);
      execute("GC.class_histogram", line -> {
         if (output != null) {
            output.println(line);
         }
         int currentLineNumber = lineNumber.incrementAndGet();
         if (currentLineNumber <= 2) {
            return;
         }

         if (line.startsWith("Total")) {
            return;
         }

         Histogram histogram = Histogram.parseLine(line, time);

         consumer.accept(line, histogram);
      });
   }

   public void disconnect() throws Exception {
      machine.detach();
   }

   @Override
   public void run() {
      try {
         while (active) {
            // System.out.print("\033[H\033[2J");
            out.println("*******************************************************************************************************************************");
            CountDownLatch latch = new CountDownLatch(1);
            Executor executor = this.executor;
            if (executor != null) {
               out.println("Executing...");
               executor.execute(() -> {
                  try {
                     out.println("Processing histogram");
                     processHistogram();
                     if (report != null) {
                        generateIndex(report, maxValues.values());
                        generateLogsView(report, processedTmes);
                     }
                  } catch (Throwable e) {
                     e.printStackTrace();
                     stop();
                  } finally {
                     latch.countDown();
                  }
               });
            }
            latch.await(1, TimeUnit.MINUTES);
            latchRunning.await(sleep, TimeUnit.MILLISECONDS);
         }
      } catch (Throwable e) {
         e.printStackTrace();
      }
   }

   public static void generateIndex(File report, Collection<Histogram> values) throws Exception {
      try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(report, "index.html"))))) {

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
            String fileName = h.fileName;
            TableGenerator.tableLine(out, makeLink("" + h.getBytes(), fileName), makeLink("" + h.getMaxBytes(), fileName), makeLink("" + h.getInstances(), fileName), makeLink("" + h.getMaxInstances(), fileName), makeLink("" + h.getTimesOver(), fileName), makeLink(h.name, fileName));
         });
         TableGenerator.tableFooter(out);

         out.println("<a href='logs.html'>Click here to view available logs</a>");
         out.println("</body>");
      }
   }

   public static void generateLogsView(File report, Collection<Long> processedTmes) throws Exception {

      try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(report, "logs.html"))))) {

         out.println("<!DOCTYPE html>");
         out.println("<html>");

         TableGenerator.addScriptHeader(out, "logsTable");

         out.println("<body>");

         out.println("<h4>histogram</hr4>");
         TableGenerator.tableBegin(out, "logsTable");
         TableGenerator.tableHeader(out, "date", "Class Histogram", "Thread Dump", "ThreadDump analyzer");

         processedTmes.forEach(time -> {
                                  String printTime = DateOps.completeDateHumanReadable(time);
                                  String histogram = DateOps.getHistogramFileName(time);
                                  String tdump = DateOps.getTDumpFileName(time);
                                  String analyzer = DateOps.getTDumpAnalyzerFileName(time);
                                  TableGenerator.tableLine(out, printTime,
                                                           "<a href='./logs/" + histogram + "'>" + histogram + "</a>",
                                                           "<a href='./logs/" + tdump + "'>" + tdump + "</a>",
                                                           "<a href='./logs/" + analyzer + "'>" + analyzer + "</a>");
                               });
         TableGenerator.tableFooter(out);

         out.println("<a href='index.html'>Histogram</a>");
         out.println("</body>");
      }

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
