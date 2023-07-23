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
import java.security.CodeSource;
import java.security.ProtectionDomain;

import picocli.AutoComplete;
import picocli.CommandLine;

@CommandLine.Command(name = "auto-complete", description = "Generates the auto complete helper file")
public class AutoCompletion implements Runnable {

   public AutoCompletion(CommandLine parent) {
      this.parent = parent;
   }

   CommandLine parent;

   @CommandLine.Parameters (description = "The script that will be used to invoke check-leak")
   File autoCompleteFile;

   public void run() {
      try {
         AutoComplete.bash("check-leak", autoCompleteFile, null, parent);
         System.out.println("Type the following commands before you can use auto-complete:");
         System.out.println("*******************************************************************************************************************************");
         System.out.println("source " + autoCompleteFile.getAbsolutePath());
         System.out.println("alias check-leak=\"java -jar " + getJarPathFromClass(AutoCompletion.class) +"\"");
         System.out.println("*******************************************************************************************************************************");

      } catch (Throwable e) {
         e.printStackTrace();
      }

   }

   private String getJarPathFromClass(Class<?> clazz) {
      ProtectionDomain protectionDomain = clazz.getProtectionDomain();
      CodeSource codeSource = protectionDomain.getCodeSource();

      if (codeSource != null) {
         return codeSource.getLocation().getPath();
      } else {
         return "check-leak*jar";
      }
   }

}
