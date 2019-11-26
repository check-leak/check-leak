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

package com.dsect.jvmti;

import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.Test;

import static com.dsect.jvmti.JVMTIInterface.noLeaks;

public class ExampleTest {

   static ArrayList<TestClass> elements = new ArrayList<>();

   /** This is just an example: this test will leak */
   @Ignore
   @Test
   public void itWillLeak() throws Exception {
      for (int i = 0; i < 10; i++) {
         elements.add(new TestClass());
      }

      noLeaks(TestClass.class.getName(), 0, 10);
   }


   @Test
   public void testProps() {
      for (String key : System.getenv().keySet()) {
         System.out.println(key + "=" + System.getenv(key));
      }
   }

   @Test
   public void testNoLeak() throws Exception {
      elements.clear();
      noLeaks(TestClass.class.getName(), 0, 10);
   }

}
