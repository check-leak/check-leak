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

import java.util.Calendar;

public class DateOps {

   public static ThreadLocal<Calendar> calendarThreadLocal = ThreadLocal.withInitial(() -> Calendar.getInstance());

   public static String getHour(long date) {
      Calendar calendar = calendarThreadLocal.get();
      calendar.setTimeInMillis(date);
      return String.format("'%02d:%02d:%02d'", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
   }

   public static String formatForFileName(long date) {
      Calendar calendar = calendarThreadLocal.get();
      calendar.setTimeInMillis(date);
      return String.format("%04d%02d%02d%02d%02d%02d%03d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND));
   }

   public static String completeDateHumanReadable(long date) {
      Calendar calendar = calendarThreadLocal.get();
      calendar.setTimeInMillis(date);
      return String.format("%04d/%02d/%02d %02d:%02d:%02d:%03d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND));
   }

   public static String getHistogramFileName(long date) {
      return "GC_histogram_" + formatForFileName(date) + ".log";
   }

   public static String getTDumpFileName(long date) {
      return "thread_dump_" + formatForFileName(date) + ".log";
   }

   public static String getTDumpAnalyzerFileName(long date) {
      return "thread_dump_" + formatForFileName(date) + ".html";
   }



}
