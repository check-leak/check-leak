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

package org.dleak.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Scanner;

import org.dleak.jvmti.JVMTIInterface;
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


      Assert.assertNotNull(JVMTIInterface.class.getResourceAsStream("/platforms-lib/darwin/libdleak.dylib"));

      Assert.assertNotNull(JVMTIInterface.class.getResource("/platforms-lib/darwin/libdleak.dylib"));
      System.out.println(JVMTIInterface.class.getResource("/platforms-lib"));
      URL url = JVMTIInterface.class.getResource("/platforms-lib/");
      System.out.println(url.getContent());
      try (InputStream libStream = url.openStream(); Scanner libScanner = new Scanner(libStream)) {
         while (libScanner.hasNextLine()) {
            String platform = libScanner.nextLine();
            System.out.println(platform);
            URL platformURL = new URL(url + "/" + platform);
            System.out.println("platformURI::" + platformURL);
            try (InputStream platformStream = platformURL.openStream(); Scanner platformScanner = new Scanner(platformStream)) {
               while (platformScanner.hasNextLine()) {
                  String binFile = platformScanner.nextLine();
                  URL binURI = new URL(platformURL + "/" + binFile);
                  System.out.println("binURI::" + binURI);
                  try (InputStream binStream = binURI.openStream()) {
                     System.out.println(binStream.getClass());
                  }
               }
            }
         }
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
