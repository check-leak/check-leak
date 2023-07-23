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

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.checkleak.core.testutil.SpawnJava;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RemoteAgentTest {

   Process process;


   public static final boolean deleteDirectory(final File directory) {
      if (directory.isDirectory()) {
         String[] files = directory.list();
         int num = 5;
         int attempts = 0;
         while (files == null && (attempts < num)) {
            try {
               Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            files = directory.list();
            attempts++;
         }

         if (files == null) {
            System.err.println("Failing list files at directory " + directory);
         } else {
            for (String file : files) {
               File f = new File(directory, file);
               deleteDirectory(f);
            }
         }
      }

      return directory.delete();
   }

   private static class Payload {
      Payload(byte[] load) {
         this.payload = load;
      }
      byte[] payload;
   }

   private static class Payload2 {
      Payload2(byte[] load) {
         this.payload = load;
      }
      byte[] payload;
   }



   public static void main(String[] arg) {
      ArrayList<Payload> garbage = new ArrayList<>();
      ArrayList<Payload2> garbage2 = new ArrayList<>();
      int i = 0;
      int j = 0;
      while (true) {
         try {
            Thread.sleep(1);
            garbage.add(new Payload(new byte[100]));
            garbage2.add(new Payload2(new byte[10]));
            if (i++ == 100) {
               System.out.println("clear garbage");
               garbage.clear();
               i = 0;
            }
            if (j++ == 1000) {
               System.out.println("clear garbage2");
               j = 0;
               garbage2.clear();
            }

         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   @AfterEach
   public void afterAll() {
      if (process != null) {
         process.destroyForcibly();
      }
   }

   @Test
   public void testRemote() throws Exception {
      process = SpawnJava.spawn(RemoteAgentTest.class.getName(), new String[]{"test"});
      Assertions.assertFalse(process.waitFor(100, TimeUnit.MILLISECONDS));
      AtomicInteger errors = new AtomicInteger(0);

      RemoteCheckLeak remoteCheckLeak = new RemoteCheckLeak();
      remoteCheckLeak.connect("" + process.pid());
      remoteCheckLeak.getHistogram(System.currentTimeMillis(), null, (line, histogram) -> {
         if (histogram.name == null || histogram.name.trim().equals("")) {
            System.out.println("Line " + line + " is generating " + histogram);
            errors.incrementAndGet();
         }
      });
      remoteCheckLeak.disconnect();
      Assertions.assertEquals(0, errors.get());
   }

   // Execute the test, look at the ./target/RemoteAgentTest and inspect the UI output
   @Test
   public void testRemoteRun() throws Exception {
      process = SpawnJava.spawn(RemoteAgentTest.class.getName(), new String[]{"test"});
      Assertions.assertFalse(process.waitFor(100, TimeUnit.MILLISECONDS));

      RemoteCheckLeak remoteCheckLeak = new RemoteCheckLeak();
      File report = new File("./target/RemoteAgentTest");
      deleteDirectory(report);
      remoteCheckLeak.setReport(report);
      remoteCheckLeak.connect("" + process.pid());

      Map<String, Histogram> histogramMap = remoteCheckLeak.parseHistogram();
      Assertions.assertFalse(histogramMap.isEmpty());

      remoteCheckLeak.setSleep(100);
      Thread t = new Thread(remoteCheckLeak);
      t.start();
      Thread.sleep(5000);
      remoteCheckLeak.stop();
      t.join(5000);
      Assertions.assertFalse(t.isAlive());
      remoteCheckLeak.disconnect();

      System.out.println("Manually inspect ./target/RemoteAgentTest");
   }

   // Execute the test, look at the ./target/RemoteAgentTest and inspect the UI output
   @Test
   public void testShouldDieAtTheEnd() throws Exception {
      process = SpawnJava.spawn(RemoteAgentTest.class.getName(), new String[]{"test"});
      Assertions.assertFalse(process.waitFor(100, TimeUnit.MILLISECONDS));

      RemoteCheckLeak remoteCheckLeak = new RemoteCheckLeak();
      File report = new File("./target/RemoteAgentTest");
      deleteDirectory(report);
      remoteCheckLeak.setReport(report);
      remoteCheckLeak.connect("" + process.pid());

      Map<String, Histogram> histogramMap = remoteCheckLeak.parseHistogram();
      Assertions.assertFalse(histogramMap.isEmpty());

      remoteCheckLeak.setSleep(100);
      Thread t = new Thread(remoteCheckLeak);
      t.start();
      Thread.sleep(500);
      process.destroyForcibly();
      t.join(5000);
      Assertions.assertFalse(t.isAlive());
      remoteCheckLeak.disconnect();
   }

   // Execute the test, look at the ./target/RemoteAgentTest and inspect the UI output
   @Test
   public void testShouldDieAtTheEnd2() throws Exception {
      process = SpawnJava.spawn(RemoteAgentTest.class.getName(), new String[]{"test"});
      Assertions.assertFalse(process.waitFor(100, TimeUnit.MILLISECONDS));

      Process checkProcess = SpawnJava.spawn(RemoteCheckLeak.class.getName(), new String[]{"remote", "--pid", "" + process.pid(), "--report", "./target/report-check", "--sleep", "1000"});
      try {
         Thread.sleep(1000);
         process.destroyForcibly();
         Assertions.assertTrue(checkProcess.waitFor(1, TimeUnit.SECONDS));
      } finally {
         checkProcess.destroyForcibly();
      }
   }

   // Execute the test, look at the ./target/RemoteAgentTest and inspect the UI output
   /*@Test
   public void testRemoteRunNoReport() throws Exception {
      process = SpawnJava.spawn(RemoteAgentTest.class.getName(), new String[]{"test"});
      Assertions.assertFalse(process.waitFor(100, TimeUnit.MILLISECONDS));

      RemoteCheckLeak remoteCheckLeak = new RemoteCheckLeak();
      remoteCheckLeak.setActive(true);
      remoteCheckLeak.startExecutor();
      remoteCheckLeak.connect("" + process.pid());

      Map<String, Histogram> histogramMap = remoteCheckLeak.parseHistogram();
      Assertions.assertFalse(histogramMap.isEmpty());

      remoteCheckLeak.setSleep(100);
      Thread t = new Thread(remoteCheckLeak);
      t.start();
      Thread.sleep(5000);
      remoteCheckLeak.stop();
      t.join(5000);
      Assertions.assertFalse(t.isAlive());
      remoteCheckLeak.disconnect();

      System.out.println("Manually inspect ./target/RemoteAgentTest");
   }*/

}
