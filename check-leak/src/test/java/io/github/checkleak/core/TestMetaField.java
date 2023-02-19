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

import java.lang.reflect.Field;

import io.github.checkleak.core.testdata.C1;
import io.github.checkleak.core.testdata.C2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This is accordingly to the specification from https://docs.oracle.com/en/java/javase/11/docs/specs/jvmti.html,
 * look for jvmtiHeapReferenceInfoField on the doc ^^
 */

public class TestMetaField {

   @Test
   public void testMeta() {
      JVMTIFieldsMetadata metadata = new JVMTIFieldsMetadata();
      Field[] fields = metadata.getFields(C1.class);
      Assertions.assertTrue(fields[2].getName().equals("a"));
      fields = metadata.getFields(C2.class);
      Assertions.assertTrue(fields[4].getName().equals("a"));
   }
}
