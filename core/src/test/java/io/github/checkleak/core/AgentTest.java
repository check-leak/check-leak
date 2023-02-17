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

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AgentTest {

   @Test
   public void printBanner() {
      Agent.printBanner(System.out);
   }

   // this is validating the RegExpression
   @Test
   public void parseArguments() {
      Map<String, String> args = Agent.parse("a1=1;a2=2;a3=\"someargument\"");
      args.forEach((a,b) -> System.out.println(a + "=" + b));
   }

   // This is validating the arguments are valid
   @Test
   public void parseParameters() {
      Map<String, String> args = Agent.parseParameters("");
      Assertions.assertEquals("60000", args.get("sleep"));

      args = Agent.parseParameters(null);
      Assertions.assertEquals("60000", args.get("sleep"));

      args = Agent.parseParameters("invalidArg=3333");
      Assertions.assertEquals("60000", args.get("sleep"));
      Assertions.assertEquals(null, args.get("invalidArg"));
   }

   @Test
   public void calculatePercentage() {
      int percentage = Agent.calculatePercentage(3000, 5000);
      Assertions.assertEquals(60, percentage);
   }
}
