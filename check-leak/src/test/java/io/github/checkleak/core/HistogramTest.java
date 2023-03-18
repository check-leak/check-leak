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
import java.util.ArrayList;

import io.github.checkleak.core.util.TableGenerator;
import org.junit.jupiter.api.Test;

public class HistogramTest {


   @Test
   public void testParseLine() throws Exception {
      String line = "   10:            18          10400  Test";
      Histogram histogram = Histogram.parseLine(line, 0);

      histogram.addHistory(new Histogram("Test", 300, 100, System.currentTimeMillis()));
      histogram.addHistory(new Histogram("Test", 400, 200, System.currentTimeMillis() + 1000));
      histogram.addHistory(new Histogram("Test", 100, 100, System.currentTimeMillis() + 2000));

      System.out.println("Histogram::" + histogram);
   }

   @Test
   public void testGenerateChat() throws Exception {
      TableGenerator.installStuff(new File("./target/output/"));
      String line = "   10:            18          10400  Test";
      Histogram histogram = Histogram.parseLine(line, 0);

      histogram.addHistory(new Histogram("Test", 300, 100, System.currentTimeMillis()));
      histogram.addHistory(new Histogram("Test", 400, 200, System.currentTimeMillis() + 1000));
      histogram.addHistory(new Histogram("Test", 100, 100, System.currentTimeMillis() + 2000));

      histogram.generateChart(new File("./target/output/test.html"));
   }


   @Test
   public void testGenerateChatBrandNew() throws Exception {
      TableGenerator.installStuff(new File("./target/output/"));
      String line = "   10:            18          10400  Test";
      Histogram histogram = Histogram.parseLine(line, 0);

      histogram.generateChart(new File("./target/output/brandNew.html"));
   }

   @Test
   public void testGenerateIndexAndChart() throws Exception {
      TableGenerator.installStuff(new File("./target/output/"));
      String line = "   10:            18          10400  Test";
      Histogram histogram = Histogram.parseLine(line, System.currentTimeMillis());
      histogram.addHistory(new Histogram("Test", 300, 100, System.currentTimeMillis()));
      histogram.addHistory(new Histogram("Test", 400, 200, System.currentTimeMillis() + 1000));
      histogram.addHistory(new Histogram("Test", 100, 100, System.currentTimeMillis() + 2000));

      ArrayList<Histogram> list = new ArrayList<>();
      list.add(histogram);
      list.add(Histogram.parseLine("   10:            18          10400  Test3", System.currentTimeMillis() + 1000));

      histogram.generateChart(new File("./target/output/1.html"));
      histogram.setFileName("1.html");

      RemoteCheckLeak.generateIndex(new File("./target/output/"), list);
      ArrayList<Long> logsView = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
         logsView.add(System.currentTimeMillis() + (i * 1000));
      }
      RemoteCheckLeak.generateLogsView(new File("./target/output/"), logsView);
   }



}
