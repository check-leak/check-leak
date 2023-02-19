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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class Installer {

   public static void main(String arg[]) {
      try {
         if (arg.length != 1) {
            printUsage();
         }
         File file =  new File(arg[0]);
         install(file);
      } catch (Throwable e) {
         e.printStackTrace();
         printUsage();
      }
   }

   private static final void printUsage() {
      System.err.println("Please pass in the target file as the argument");
      System.exit(-1);
   }

   /** copy the binary library as this targetFile
    * @param target The file that should be generated with the proper binar for the current system.
    * @throws IOException if an issue happened during the reading and write of the binary. */
   public static void install(File target) throws IOException {
      String osName = System.getProperty("os.name").toLowerCase();
      String osArch = System.getProperty("os.arch").toLowerCase();
      String libraryName;
      if (osName.contains("windows")) {
         osName = "windows";
         libraryName = "checkleak.dll";
      } else if (osName.contains("mac")) {
         osName = "darwin-" + osArch;
         libraryName = "libcheckleak.dylib";
      } else {
         osName = "linux-amd64";
         libraryName = "libcheckleak.so";
      }

      try (InputStream inputStream = CheckLeak.class.getResourceAsStream("/platforms-lib/" + osName + "/" + libraryName);
           OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(target))) {
         copy(inputStream, outputStream);
      }
   }


   protected static void copy(InputStream is, OutputStream os) throws IOException {
      byte[] buffer = new byte[1024 * 4];
      int c = is.read(buffer);
      while (c >= 0) {
         os.write(buffer, 0, c);
         c = is.read(buffer);
      }
   }


}
