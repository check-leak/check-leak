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
      ArrayList<Payload2> permanent = new ArrayList<>();
      int i = 0;
      while (true) {
         try {
            Thread.sleep(1);
            garbage.add(new Payload(new byte[100]));
            permanent.add(new Payload2(new byte[10]));
            if (i++ == 100) {
               garbage.clear();
               i = 0;
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
      remoteCheckLeak.getHistogram(System.currentTimeMillis(), (line, histogram) -> {
         if (histogram.name == null || histogram.name.trim().equals("")) {
            System.out.println("Line " + line + " is generating " + histogram);
            errors.incrementAndGet();
         }
      });
      remoteCheckLeak.disconnect();
      Assertions.assertEquals(0, errors.get());
   }

   @Test
   public void testRemoteRun() throws Exception {
      process = SpawnJava.spawn(RemoteAgentTest.class.getName(), new String[]{"test"});
      Assertions.assertFalse(process.waitFor(100, TimeUnit.MILLISECONDS));

      RemoteCheckLeak remoteCheckLeak = new RemoteCheckLeak();
      remoteCheckLeak.setActive(true);
      File report = new File("./target/RemoteAgentTest");
      deleteDirectory(report);
      remoteCheckLeak.setReport(report);
      remoteCheckLeak.connect("" + process.pid());
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

   @Test
   public void testParameter() throws Exception {
      RemoteCheckLeak.Parameters x = new RemoteCheckLeak.Parameters();
      x.parse("check", "-sleep", "300", "-pid", "333");
      Assertions.assertEquals("333", x.pid);
      Assertions.assertEquals(300, x.sleep);
      x.parse("check", "-pid", "177", "-sleep", "277");
      Assertions.assertEquals("177", x.pid);
      Assertions.assertEquals(277, x.sleep);
   }

}
