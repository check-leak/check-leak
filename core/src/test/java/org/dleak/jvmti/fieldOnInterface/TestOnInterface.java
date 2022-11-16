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

package org.dleak.jvmti.fieldOnInterface;

import org.dleak.jvmti.JVMTIInterface;
import org.dleak.jvmti.TestClass;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Clebert Suconic
 */

public class TestOnInterface {

   static DeclaringClass declaringClass;


   @Test
   public void testOnInterface() throws Exception {
      declaringClass = new DeclaringClass();

      JVMTIInterface jvmtiInterface = new JVMTIInterface();
      jvmtiInterface.getAllObjects(TestClass.class);

      final String reportOnSuper = jvmtiInterface.exploreObjectReferences(10, 1, false, declaringClass.onSuper);
      System.out.println("Report = " + reportOnSuper);
      Assert.assertTrue(reportOnSuper.contains("onSuper"));

      final String reportOnClass = jvmtiInterface.exploreObjectReferences(10, 1, false, declaringClass.declared);
      System.out.println("Report = " + reportOnClass);
      final String reportOnInterface = jvmtiInterface.exploreObjectReferences(10, 1, false, declaringClass.onInterface);
      System.out.println("Report = " + reportOnInterface);
      final String reportOnSuperClassInterface = jvmtiInterface.exploreObjectReferences(10, 1, false, declaringClass.onSuperInterface);
      System.out.println("Report = " + reportOnSuperClassInterface);
      final String reportOnSecondSuperClassInterface = jvmtiInterface.exploreObjectReferences(10, 1, false, declaringClass.secondInterfaceOnSuper);
      System.out.println("Report = " + reportOnSecondSuperClassInterface);
      Assert.assertTrue(reportOnInterface.contains("onInterface"));
      Assert.assertTrue(reportOnClass.contains("declared"));
      Assert.assertTrue(reportOnSuperClassInterface.contains("onSuperInterface"));
      Assert.assertTrue(reportOnSecondSuperClassInterface.contains("secondInterfaceOnSuper"));

   }

}
