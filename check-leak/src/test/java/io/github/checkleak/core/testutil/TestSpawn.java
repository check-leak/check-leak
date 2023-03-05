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

package io.github.checkleak.core.testutil;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSpawn {

   public static void main(String arg[]) {
      while (true) {
         try {
            System.out.println("hello");
            Thread.sleep(1000);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }


   @Test
   public void testSpawn() throws Exception {
      Process p = SpawnJava.spawn(TestSpawn.class.getName(), new String[]{"hello"});
      try {
         Assertions.assertFalse(p.waitFor(100, TimeUnit.MILLISECONDS));
         p.destroyForcibly();
      } finally {
         if (p != null) {
            try {
               p.destroyForcibly();
            } catch (Throwable ignored) {
            }
         }
      }

   }

}
