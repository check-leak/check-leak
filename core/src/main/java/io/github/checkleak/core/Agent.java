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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Agent implements java.lang.instrument.ClassFileTransformer, Runnable {
   SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

   final PrintStream out;
   final int downPercentage;
   final int sleep;

   public Agent(int sleep, String fileOutput, int downPercentage) throws Exception {
      this.sleep = sleep;
      if (fileOutput == null) {
         out = System.out;
      } else {
         FileOutputStream fileout = new FileOutputStream(fileOutput);
         out = new PrintStream(fileout);
      }

      this.downPercentage = downPercentage;
   }

   private static void logError(String logging) {
      System.err.println("CheckLeak Agent::" + logging);
   }

   private static void logInfo(String logging) {
      System.out.println("CheckLeak Agent::" + logging);
   }


   CheckLeak checkLeak = new CheckLeak();
   HashMap<String, DataPoint> dataPoints = new HashMap<>();

   private static class DefaultValue {
      String defaultValue;
      String explanation;

      DefaultValue(String a, String b) {
         this.defaultValue = a;
         this.explanation = b;
      }

      @Override
      public String toString() {
         return defaultValue + "// " + explanation;
      }
   }

   public static int calculatePercentage(long current, long max)  {
      double percentage = ((double)current) / ((double) max);
      percentage *= 100;
      return (int) percentage;
   }


   static HashMap<String, DefaultValue> validProperties = new HashMap();
   static {
      validProperties.put("sleep", new DefaultValue("60000", "Time to sleep in milliseconds"));
      validProperties.put("output", new DefaultValue(null, "The file output. default=null which means System.out"));
      validProperties.put("down", new DefaultValue("-1", "How much percentage to consider a retracted value. We only consider retracted at a certain percentage of the max value. Default is -1 (disabled)"));
   }

   class DataPoint {
      String clazz;
      int instances;
      long bytes;

      public DataPoint(Class clazz, InventoryDataPoint dataPoint) {
         this.clazz = clazz.getName();
         this.instances = dataPoint.getInstances();
         this.bytes = dataPoint.getBytes();
      }

      public void check(InventoryDataPoint currentDataPoint) {
         boolean changed = false;
         if (currentDataPoint.getBytes() > bytes) {
            changed = true;
         }
         if (currentDataPoint.getInstances() > instances) {
            changed = true;
         }

         if (downPercentage >= 0) {
            if (currentDataPoint.getBytes() < bytes && calculatePercentage(currentDataPoint.getBytes(), bytes) <= downPercentage) {
               changed = true;
            }
            if (currentDataPoint.getInstances() < instances && calculatePercentage(currentDataPoint.getBytes(), instances) <= downPercentage) {
               changed = true;
            }
         }

         if (changed) {
            onChange(false, currentDataPoint);
         }
      }

      private void onChange(boolean newItem, InventoryDataPoint dataPoint) {
         if (newItem) {
            out.println("|*new* " + clazz + "|" + bytes + " bytes | " + instances + " instances|");
         } else {
            long diffBytes = dataPoint.getBytes() - bytes;
            int diffInstances = dataPoint.getInstances() - instances;
            out.println("|" + clazz + "|" + bytes + " bytes (variance=" + diffBytes + ") | " + instances + " instances (variance=" + diffInstances + ")|");
         }
         this.bytes = dataPoint.getBytes();
         this.instances = dataPoint.getInstances();
      }
   }

   class CompareDataPoint implements Comparator<InventoryDataPoint> {
      @Override
      public int compare(InventoryDataPoint o1, InventoryDataPoint o2) {
         if (o2.getBytes() > o1.getBytes()) {
            return 1;
         } else if (o2.getBytes() < o1.getBytes()) {
            return -1;
         } else {
            return o1.getClazz().getName().compareTo(o2.getClazz().getName());
         }
      }
   }

   public void run() {

      try {
         Thread.currentThread().setName("CheckLeak Tick Agent");
         long tickNumber = 0;
         while (true) {
            Thread.sleep(sleep);
            try {
               out.println("*******************************************************************************************************************************");
               out.println("Check-Leak Agent");
               out.println(dateFormat.format(new Date()));
               Map<Class<?>, InventoryDataPoint> inventoryDataPointMap = checkLeak.produceInventory();
               ArrayList<InventoryDataPoint> list = new ArrayList<InventoryDataPoint>(inventoryDataPointMap.values());
               Collections.sort(list, new CompareDataPoint());
               list.forEach(b -> {
                  DataPoint dataPoint = dataPoints.get(b.getClazz().getName());
                  if (dataPoint == null) {
                     dataPoint = new DataPoint(b.getClazz(), b);
                     dataPoints.put(b.getClazz().getName(), dataPoint);
                     dataPoint.onChange(true, b);
                  } else {
                     dataPoint.check(b);
                  }
               });
               list.clear();
               inventoryDataPointMap.clear(); // a little help to the GC
               inventoryDataPointMap = null; // I know the VM is supposed to clear this after I leave this loop, but I'm not a believer and my OCD urges me to clear this reference.
               list = null;
            } catch (Throwable e) {
               e.printStackTrace();
            }
         }
      } catch (Throwable e) {
         e.printStackTrace();
      }
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

   public static HashMap<String, String> parse(String agentArgs) {
      HashMap<String, String> properties = new HashMap<String, String>();
      Pattern regex = Pattern.compile("(?<=^|;)([^=;]+)=(\"[^\"]*\"|[^;]*)");
      Matcher regexMatcher = regex.matcher(agentArgs);
      while (regexMatcher.find()) {
         String key = regexMatcher.group(1);
         String value = regexMatcher.group(2);
         if (value.startsWith("\"") && value.endsWith("\"")) {
            // If the value is a quoted string, remove the quotes
            value = value.substring(1, value.length() - 1);
         }
         properties.put(key, value);
      }
      return properties;
   }

   public static HashMap<String, String> parseParameters(String agentArgs) {
      if (agentArgs == null) {
         agentArgs = "";
      } else {
         agentArgs = agentArgs.trim();
      }

      HashMap<String, String> values = parse(agentArgs);
      HashMap<String, String> outputValues = new HashMap<>();

      values.forEach((a, b) -> {
         if (!validProperties.containsKey(a)) {
            logError("InvalidProperty " + a);
         } else {
            outputValues.put(a, b);
         }
      });

      validProperties.forEach((a, b) -> {
         if (!outputValues.containsKey(a)) {
            logInfo("Applying default on property " + a +"=" + b.defaultValue + " //" + b.explanation);
            outputValues.put(a, b.defaultValue);
         }
      });

      return outputValues;
   }

   public static void printValues(HashMap<String, String> values) {
      values.forEach((a, b) -> {
         DefaultValue defaultValue = validProperties.get(a);
         logInfo(a + "=" + b + " // " + defaultValue);
      });
   }


   public static void premain(String agentArgs, Instrumentation inst) {
      printBanner(System.out);

      HashMap<String, String> args = parseParameters(agentArgs);
      printValues(args);


      int sleep = Integer.parseInt(args.get("sleep"));
      String fileOutput = args.get("output");
      int downPercentage = Integer.parseInt(args.get("down"));

      logInfo("sleep=" + sleep + ", downPercentage=" + downPercentage + ", fileOutput=" + fileOutput);

      try {
         Agent agent = new Agent(sleep, fileOutput, downPercentage);
         Thread t = new Thread(agent);
         t.setDaemon(true);
         t.start();
      } catch (Exception e) {
         e.printStackTrace();
         logError("Invalid initializational " + e);
      }
   }
}
