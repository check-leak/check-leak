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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HTMLHelper {

   public static void copy(String name, File directory) throws Exception {
      directory.mkdirs();
      InputStream stream = HTMLHelper.class.getResourceAsStream(name);
      File file = new File(directory, name);
      copy(stream, new FileOutputStream(file));
   }


   public static void copy(InputStream is, OutputStream os) throws IOException {
      byte[] buffer = new byte[1024 * 4];
      int c = is.read(buffer);
      while (c >= 0) {
         os.write(buffer, 0, c);
         c = is.read(buffer);
      }
   }

   public static String makeLink(String text, String resouce) {
      return "<a href='" + resouce + "'>" + text + "</a>";
   }


   protected static String readTextFile(String source) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (InputStream in = openStream(source)) {
         if (in == null) {
            throw new IOException("could not find resource " + source);
         }
         copy(in, out);
      }
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
   }


   protected static InputStream openStream(String source) {
      return HTMLHelper.class.getResourceAsStream(source);
   }
}