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

package com.dsect.jvmti;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** This is implemented accordingly to */
public class JVMTIFieldsMetadata {

   Map<Class, Field[]> fieldsMetaData = new HashMap<>();

   public void clear() {
      fieldsMetaData.clear();
   }

   public Field[] getFields(Class clazz) {
      Field[] fields = fieldsMetaData.get(clazz);

      if (fields == null) {

         ArrayList<Field> fieldArray = new ArrayList<>();

         getClassFieldsOnInterfaces(fieldArray, clazz);
         getClassFields(fieldArray, clazz);
         fields = fieldArray.toArray(new Field[fieldArray.size()]);
         fieldsMetaData.put(clazz, fields);
      }

      return fields;
   }


   void getClassFieldsOnInterfaces(ArrayList<Field> fieldList, Class clazz) {

      if (clazz.getSuperclass() != null) {
         getClassFieldsOnInterfaces(fieldList, clazz.getSuperclass());
      }


      Class[] interfaces = clazz.getInterfaces();

      for (Class interfaceReading : interfaces) {
         getClassFields(fieldList, interfaceReading);
      }

   }

   void getClassFields(ArrayList<Field> fieldList, Class clazz) {

      if (clazz.getSuperclass() != null) {
         getClassFields(fieldList, clazz.getSuperclass());
      }

      if (clazz.isInterface()) {
         for (Class superInterface : clazz.getInterfaces()) {
            if (superInterface == clazz) {
               continue;
            }
            getClassFields(fieldList, superInterface);
         }

      }

      for (Field field : clazz.getDeclaredFields()) {
         fieldList.add(field);
      }


   }

}
