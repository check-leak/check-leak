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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class FindHolderTest {

   @Test
   public void testLoaded() {
      Assert.assertTrue(JVMTIInterface.isLoaded());
   }

   @Test
   public void testGetObjects() {

      TestClass testClass = new TestClass();
      testClass.someString = "Hello Francis!!!";

      JVMTIInterface jvmtiInterface = new JVMTIInterface();
      Object[] objects = jvmtiInterface.getAllObjects(TestClass.class);

      Assert.assertEquals(1, objects.length);
      Assert.assertTrue(objects[0] instanceof TestClass);

      Assert.assertSame(testClass, objects[0]);

      Assert.assertEquals("Hello Francis!!!", ((TestClass)objects[0]).someString);

      /*{
         for (Object holder : jvmtiInterface.getReferenceHolders(new Object[]{testClass})) {
            System.out.println("object is being held at " + holder);
         }
      } */

      objects = null;
      testClass = null;


      objects = jvmtiInterface.getAllObjects(TestClass.class);
      Assert.assertEquals(0, objects.length);
   }


   /** This method will show an object leaking, and a report to where is the reference on it */
   @Test
   public void testShowReferencing() throws Exception {

      TestClass testClass = new TestClass();
      testClass.someString = "Hello Francis!!!";

      JVMTIInterface jvmtiInterface = new JVMTIInterface();

      System.out.println(jvmtiInterface.exploreObjectReferences(10, true, testClass));


   }
   /** This method will show an object leaking, and a report to where is the reference on it */
   @Test
   public void testNoLeak() throws Exception {

      TestClass testClass = new TestClass();
      testClass.someString = "Hello Francis!!!";

      boolean leaked = false;

      try {
         JVMTIInterface.noLeaks(TestClass.class.getName(), 0, 10);
      } catch (UnexpectedLeak leak) {
         leak.printStackTrace();
         leaked = true;
      }


      Assert.assertTrue(leaked);

      testClass = null;

      JVMTIInterface.noLeaks(TestClass.class.getName(), 0, 10);
   }
}
