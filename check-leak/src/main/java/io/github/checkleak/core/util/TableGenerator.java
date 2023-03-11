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

import java.io.File;
import java.io.PrintStream;

import static io.github.checkleak.core.util.HTMLHelper.copy;

public class TableGenerator {

   public static void installStuff(File report) throws Exception {

      File styleDirectory = new File(report, "styles");
      File imageDirectory = new File(report, "images");

      copy("framework.css", styleDirectory);
      copy("jquery.dataTables.min.css", styleDirectory);
      copy("jquery.dataTables.min.js", styleDirectory);
      copy("jquery.min.js", styleDirectory);

      copy("sort_both.png", imageDirectory);
      copy("sort_asc.png", imageDirectory);
      copy("sort_desc.png", imageDirectory);
   }

   public static void addScriptHeader(PrintStream output, String... tableName) {
      styleAddScriptHeader(output, "./styles/", tableName);
   }

   public static void styleAddScriptHeader(PrintStream output, String stylesFolder, String... tableName) {
      output.println("<link type=\"text/css\" charset=\"utf8\" rel=\"stylesheet\" href=\"" + stylesFolder + "jquery.dataTables.min.css\">");
      output.println("<script type=\"text/javascript\" charset=\"utf8\" src=\"" + stylesFolder + "jquery.min.js\"></script>");
      output.println("<script type=\"text/javascript\" charset=\"utf8\" src=\"" + stylesFolder + "jquery.dataTables.min.js\"></script>");
      output.println("<script type=\"text/javascript\" class=\"init\">");
      output.println("$(document).ready( function () {");
      for (String t : tableName) {
         output.println("$(\"#" + t + "\").DataTable();");
      }
      output.println("} );");
      output.println("</script>");
   }


   public static void tableBegin(PrintStream output, String tableName) {
      output.println("<table id=\"" + tableName + "\" class=\"display\">");
   }

   public static void tableHeader(PrintStream output, String ... headers) {
      output.print("<thead><tr>");
      for (String h : headers) {
         output.print("<th>" + h + "</th>");
      }

      output.println("</tr></thead>");
      output.println("<tbody>");
   }

   public static void tableLine(PrintStream output, String ... rows) {

      output.print("<tr>");
      for (String r : rows) {
         output.print("<td>" + r + "</td>");
      }
      output.println("</tr>");

   }

   public static void tableFooter(PrintStream output) {
      output.println("</tbody></table>");
   }

}
