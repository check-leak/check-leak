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

package io.github.checkleak.core.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import static io.github.checkleak.core.util.HTMLHelper.copy;

public class TDumpAnalyzer {

   public static String dumpFile;

   public static void installStuff(File target) throws Exception {
      target.mkdirs();
      copy("analyze.js", target);
      copy("code-stylesheet.css", target);
      copy("tdumpAnalyzer.html", target);
      dumpFile = HTMLHelper.readTextFile("tdumpAnalyzer.html");
   }

   public static void installDump(File plainFile, File htmlFile, String dump) throws Exception {
      printFile(plainFile, dump);

      String newDump = dumpFile.replace("REPLACE_DUMP", dump);

      printFile(htmlFile, newDump);
   }

   private static void printFile(File file, String output) throws Exception {
      try (PrintStream  stream = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)))) {
         stream.print(output);
      }
   }

}
