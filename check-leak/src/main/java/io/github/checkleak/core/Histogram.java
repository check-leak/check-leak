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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.checkleak.core.util.DateOps;
import io.github.checkleak.core.util.TableGenerator;

public class Histogram {

   static AtomicInteger sequenceGenerator = new AtomicInteger(1);

   public Histogram(String name, long bytes, long instances, long time) {
      this.name = name;
      this.bytes = bytes;
      this.maxBytes = bytes;
      this.instances = instances;
      this.maxInstances = instances;
      this.time = time;
   }

   public Histogram copy() {
      return new Histogram(null, bytes, instances, time);
   }

   String name;
   long bytes;
   long maxBytes;
   long instances;
   long maxInstances;
   long time;
   long timesOver;
   String fileName;

   public long getTime() {
      return time;
   }

   public long getTimesOver() {
      return timesOver;
   }

   public String getFileName() {
      return fileName;
   }

   public Histogram setFileName(String fileName) {
      this.fileName = fileName;
      return this;
   }

   ArrayList<Histogram> history = new ArrayList<>();


   public String getName() {
      return name;
   }

   public Histogram setName(String name) {
      this.name = name;
      return this;
   }

   public long getBytes() {
      return bytes;
   }

   public long getMaxBytes() {
      return maxBytes;
   }

   public long getMaxInstances() {
      return maxInstances;
   }

   public Histogram setBytes(long bytes) {
      this.bytes = bytes;
      return this;
   }

   public long getInstances() {
      return instances;
   }

   public Histogram setInstances(long instances) {
      this.instances = instances;
      return this;
   }

   public static Histogram parseLine(String line, long time) {

      // Remove leading and trailing whitespaces
      line = line.trim();

      // Split the string by spaces
      String[] values = line.split("\\s+");

      // Extract the values from the array
      String sequence = values[0];
      String numElements = values[1];
      String numBytes = values[2];
      String name = String.join(" ", Arrays.copyOfRange(values, 3, values.length));

      return new Histogram(name, Long.parseLong(numBytes), Long.parseLong(numElements), time);
   }


   public void check(Histogram currentDataPoint, File report, Executor executor, PrintStream out) throws Exception {
      boolean overBytes = false, overInstances = false;
      boolean changeBytes = false, changeInstances = false;
      if (currentDataPoint.getBytes() > getMaxBytes()) {
         overBytes = true;
      }
      if (currentDataPoint.getInstances() > getMaxInstances()) {
         overInstances = true;
      }

      if (currentDataPoint.getInstances() != instances) {
         changeInstances = true;
      }

      if (currentDataPoint.getBytes() != bytes) {
         changeBytes = true;
      }

      if (overBytes || overInstances) {
         onOver(false, currentDataPoint, report, executor, out);
      }

      this.bytes = currentDataPoint.bytes;
      this.instances = currentDataPoint.instances;

      if (changeBytes || changeInstances) {
         addHistory(currentDataPoint);
         generateReport(report, executor);
      }
   }

   private void generateReport(File report, Executor executor) {
      if (report != null) {
         if (fileName == null) {
            fileName = "charts/" + sequenceGenerator.incrementAndGet() + ".html";
         }
         executor.execute(() -> {
            try {
               generateChart(new File(report, fileName));
            } catch (Exception e) {
               e.printStackTrace();
            }
         });
      }
   }

   public void addHistory(Histogram dataPoint) {
      history.add(dataPoint);
   }

   private String logLinks(long time) {
      return "  <a href='../logs/" + DateOps.getHistogramFileName(time) + "'>Histogram</a>" +
         "  <a href='../logs/" + DateOps.getTDumpFileName(time) + "'>Tdump</a>" +
         "  <a href='../logs/" + DateOps.getTDumpAnalyzerFileName(time) + "'>Analyzer</a>";
   }

   public void generateChart(File output) throws Exception {
      output.getParentFile().mkdirs();

      PrintStream stream = new PrintStream(new FileOutputStream(output));
      stream.println("<!DOCTYPE html>");
      stream.println("<html>");
      stream.println("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>");
      TableGenerator.styleAddScriptHeader(stream, "../styles/", "histogram");
      stream.println("<body>");
      stream.println("<h1>Allocations for " + name + "</h2>");
      stream.println("<div id=\"bytesChart\" style=\"width:100%; max-width:1600px; height:500px;\"></div> ");
      stream.println("<div id=\"instancesChart\" style=\"width:100%; max-width:1600px; height:500px;\"></div> ");

      TableGenerator.tableBegin(stream, "histogram");
      TableGenerator.tableHeader(stream, "Time", "Bytes", "Instances", "links");
      history.forEach((histogram -> {
         TableGenerator.tableLine(stream, DateOps.completeDateHumanReadable(histogram.time), "" + histogram.getBytes(), "" + histogram.getInstances(), logLinks(histogram.time));
      }));
      TableGenerator.tableFooter(stream);


      stream.println("<script>");;
      stream.println("google.charts.load('current', {'packages':['corechart']});");
      stream.println("google.charts.setOnLoadCallback(drawChart);");

      stream.println("function drawChart() {");

      stream.println("var dataBytesArray = google.visualization.arrayToDataTable([");
      stream.println("['Date', 'link', 'Bytes']");
      history.forEach(histogram -> {
         stream.println(",[" + DateOps.getHour(histogram.time) + "," + "'../logs/" + DateOps.getTDumpAnalyzerFileName(histogram.time) + "',"  + histogram.bytes + "]");
      });
      stream.println("]);");
      stream.println("var dataBytes = new google.visualization.DataView(dataBytesArray);dataBytes.setColumns([0, 2]);");

      stream.println("var dataInstancesArray = google.visualization.arrayToDataTable([");
      stream.println("['Date', 'link', 'Instances']");
      history.forEach(histogram -> {
         stream.println(",[" + DateOps.getHour(histogram.time) + "," + "'../logs/" + DateOps.getTDumpAnalyzerFileName(histogram.time) + "',"  + histogram.instances + "]");
      });
      stream.println("]);");
      stream.println("var dataInstances = new google.visualization.DataView(dataInstancesArray);dataInstances.setColumns([0, 2]);");

      stream.println("var optionsBytes = {");
      stream.println(" title: 'Bytes allocated',");
      stream.println(" hAxis: {title: 'Time'},");
      stream.println(" vAxis: {title: 'Bytes'},");
      stream.println(" legend: 'none'");
      stream.println("};");


      stream.println("var optionsInstances = {");
      stream.println(" title: 'Instances allocated',");
      stream.println(" hAxis: {title: 'Time'},");
      stream.println(" vAxis: {title: 'Instances'},");
      stream.println(" legend: 'none'");
      stream.println("};");

      stream.println("var chart = new google.visualization.LineChart(document.getElementById('bytesChart'));");
      stream.println("chart.draw(dataBytes, optionsBytes);");

      stream.println("var chart2 = new google.visualization.LineChart(document.getElementById('instancesChart'));");
      stream.println("chart2.draw(dataInstances, optionsInstances);");

      stream.println("var selectHandlerBytes = function(e) {window.location = dataBytesArray.getValue(chart.getSelection()[0]['row'], 1 );}");
      stream.println("var selectHandlerInstances = function(e) {window.location = dataInstancesArray.getValue(chart.getSelection()[0]['row'], 1 );}");

      stream.println("google.visualization.events.addListener(chart, 'select', selectHandlerBytes);");
      stream.println("google.visualization.events.addListener(chart2, 'select', selectHandlerInstances);");

      stream.println("}");
      stream.println("</script>");
      stream.println("</body>");
      stream.println("</html>");

      stream.close();
   }

   public void onOver(boolean newItem, Histogram dataPoint, File report, Executor executor, PrintStream out) throws Exception {
      timesOver++;
      if (newItem) {
         out.println(String.format("|%30s|%30s|%s", maxBytes + " bytes", maxInstances + " instances", name));
      } else {
         long diffBytes = dataPoint.getMaxBytes() - bytes;
         long diffInstances = dataPoint.getMaxInstances() - instances;
         String diffBytesStr = " (" + (diffBytes > 0 ? "+" : "") + diffBytes + ")";
         String diffInstancesStr = " (" + (diffInstances > 0 ? "+" : "") + diffInstances + ")";
         out.println(String.format("|%30s|%30s|%s", maxBytes + " bytes" + diffBytesStr, maxInstances + " instances" + diffInstancesStr, name));
      }
      if (dataPoint.getBytes() > this.maxBytes) {
         this.maxBytes = dataPoint.getMaxBytes();
      }
      if (dataPoint.getInstances() > this.maxInstances) {
         this.maxInstances = dataPoint.maxInstances;
      }
      if (newItem) { // just so we will have a file for every class
         generateReport(report, executor);
      }
   }


   @Override
   public String toString() {
      return "Histogram{" + "name='" + name + '\'' + ", bytes=" + bytes + ", instances=" + instances + '}';
   }
}
