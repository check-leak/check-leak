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

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import io.github.checkleak.core.InventoryDataPoint;
import io.github.checkleak.core.CheckLeak;

/**
 * Utility methods and reports.
 * */
public class JVMTIReport {

   public static boolean hasLeaks(String clazzName,
                              int expectedInstances,
                              int reportDepth) throws Exception {
      CheckLeak checkLeak = new CheckLeak();
      checkLeak.forceGC();

      Object[] objects = null;
      objects = checkLeak.getAllObjects(clazzName);

      if (objects.length > expectedInstances) {
         if (reportDepth > 0) {
            String report = checkLeak.findRoots(reportDepth, true, objects);
            System.out.println("Root reference of " + clazzName + ":\n" + report);

            report = checkLeak.exploreObjectReferences(reportDepth, 1, false, objects);
            System.out.println(report);
         }
         return true;
      } else {
         return false;
      }

   }


   /**
    * Will list the current memory inventory.
    * @param html should the output be an html table
    * @return the report
    * @throws Exception in case any issues happened.
    */
   public static synchronized String inventoryReport(final boolean html) throws Exception {
      CheckLeak checkLeak = new CheckLeak();
      Map map = checkLeak.produceInventory();

      TreeSet valuesSet = new TreeSet(map.values());
      Iterator iterDataPoints = valuesSet.iterator();
      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      if (html) {
         out.println("<table><tr><td>Class</td><td>#Instances</td><td>#Bytes</td></tr>");
      } else {
         out.println(String.format("|%1$-100s|%2$10s|%3$10s|", "Class", "Instances", "Bytes"));
      }

      while (iterDataPoints.hasNext()) {
         InventoryDataPoint point = (InventoryDataPoint) iterDataPoints.next();
         if (html) {
            out.println("<tr><td>" + point.getClazz().getName() + "</td><td>" + point.getInstances() + "</td><td>" + point.getBytes() + "</td></tr>");
         } else {
            out.println(String.format("|%1$-100s|%2$10d|%3$10d|", point.getClazz().getName(), point.getInstances(), point.getBytes()));
         }
      }

      if (html) {
         out.println("</table>");
      }

      return charArray.toString();
   }


}
