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

import java.nio.file.Paths;
import java.util.ArrayList;

public class SpawnJava {

   public static Process spawn(String className, String[] args) throws Exception {
      String classPath = System.getProperty("java.class.path");
      final String javaPath = Paths.get(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString();
      ProcessBuilder builder = new ProcessBuilder();
      ArrayList<String> command = new ArrayList<>();
      command.add(javaPath);
      command.add("-Xmx512m");
      command.add("-Xms512m");
      command.add(className);
      if (args != null) {
         for (String arg : args) {
            command.add(arg);
         }
      }
      builder.command(command);
      builder.environment().put("CLASSPATH", classPath);
      builder.inheritIO();
      return builder.start();
   }

}
