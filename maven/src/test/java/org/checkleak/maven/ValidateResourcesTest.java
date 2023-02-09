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

package org.checkleak.maven;

import java.io.InputStream;

import org.checkleak.jvmti.JVMTIInterface;
import org.junit.Assert;
import org.junit.Test;

public class ValidateResourcesTest {

   @Test
   public void testValidateResources() throws Exception {
      System.getenv().forEach((a, b) -> {
         System.out.println("env " + a + " = " + b);
      });

      System.getProperties().forEach((a, b) -> {
         System.out.println("property " + a + " = " + b);
      });


      InputStream inputStream = JVMTIInterface.class.getResourceAsStream("/platforms-lib/darwin/libcheckleak.dylib");
      Assert.assertNotNull(inputStream);
      inputStream.close();
   }
}
