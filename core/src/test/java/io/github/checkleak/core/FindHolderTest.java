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

import io.github.checkleak.core.testdata.TestClass;
import io.github.checkleak.core.util.JVMTIReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FindHolderTest {

   @Test
   public void testLoaded() {
      Assertions.assertTrue(CheckLeak.isLoaded());
   }

   @Test
   public void testGetObjects() {

      TestClass testClass = new TestClass(null);
      testClass.someString = "Hello Francis!!!";

      CheckLeak checkLeak = new CheckLeak();
      Object[] objects = checkLeak.getAllObjects(TestClass.class);

      Assertions.assertEquals(1, objects.length);
      Assertions.assertTrue(objects[0] instanceof TestClass);

      Assertions.assertSame(testClass, objects[0]);

      Assertions.assertEquals("Hello Francis!!!", ((TestClass)objects[0]).someString);

      /*{
         for (Object holder : jvmtiInterface.getReferenceHolders(new Object[]{testClass})) {
            System.out.println("object is being held at " + holder);
         }
      } */

      objects = null;
      testClass = null;


      objects = checkLeak.getAllObjects(TestClass.class);
      Assertions.assertEquals(0, objects.length);
   }


   /** This method will show an object leaking, and a report to where is the reference on it */
   @Test
   public void testShowReferencing() throws Exception {

      TestClass testClass = new TestClass(null);
      testClass.someString = "Hello Francis!!!";

      CheckLeak checkLeak = new CheckLeak();

      System.out.println(checkLeak.exploreObjectReferences(10, 10, true, testClass));


   }
   /** This method will show an object leaking, and a report to where is the reference on it */
   @Test
   public void testNoLeak() throws Exception {

      TestClass testClass = new TestClass(null);
      testClass.someString = "Hello Francis!!!";

      boolean leaked = JVMTIReport.hasLeaks(TestClass.class.getName(), 0, 10);

      Assertions.assertTrue(leaked);

      testClass = null;

      Assertions.assertFalse(JVMTIReport.hasLeaks(TestClass.class.getName(), 0, 10));
   }
}
