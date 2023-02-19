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

package io.github.checkleak.core.fieldOnInterface;

import io.github.checkleak.core.CheckLeak;
import io.github.checkleak.core.testdata.TestClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Clebert Suconic
 */

public class InterfaceTest {

   static DeclaringClass declaringClass;


   @Test
   public void testOnInterface() throws Exception {
      declaringClass = new DeclaringClass();

      CheckLeak checkLeak = new CheckLeak();
      checkLeak.getAllObjects(TestClass.class);

      final String reportOnSuper = checkLeak.exploreObjectReferences(10, 1, false, declaringClass.onSuper);
      System.out.println("Report = " + reportOnSuper);
      Assertions.assertTrue(reportOnSuper.contains("onSuper"));

      final String reportOnClass = checkLeak.exploreObjectReferences(10, 1, false, declaringClass.declared);
      System.out.println("Report = " + reportOnClass);
      final String reportOnInterface = checkLeak.exploreObjectReferences(10, 1, false, declaringClass.onInterface);
      System.out.println("Report = " + reportOnInterface);
      final String reportOnSuperClassInterface = checkLeak.exploreObjectReferences(10, 1, false, declaringClass.onSuperInterface);
      System.out.println("Report = " + reportOnSuperClassInterface);
      final String reportOnSecondSuperClassInterface = checkLeak.exploreObjectReferences(10, 1, false, declaringClass.secondInterfaceOnSuper);
      System.out.println("Report = " + reportOnSecondSuperClassInterface);
      Assertions.assertTrue(reportOnInterface.contains("onInterface"));
      Assertions.assertTrue(reportOnClass.contains("declared"));
      Assertions.assertTrue(reportOnSuperClassInterface.contains("onSuperInterface"));
      Assertions.assertTrue(reportOnSecondSuperClassInterface.contains("secondInterfaceOnSuper"));

   }

}
