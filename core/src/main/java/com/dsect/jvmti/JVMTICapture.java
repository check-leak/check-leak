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

import java.util.HashMap;
import java.util.WeakHashMap;

public class JVMTICapture implements JVMTICallBack {

   HashMap<Long, Class<?>> classesMap = new HashMap<Long, Class<?>>();

   WeakHashMap<Class<?>, InventoryDataPoint> maps = new WeakHashMap<Class<?>, InventoryDataPoint>();

   public void notifyClass(final long classTag, final Class<?> clazz) {
      classesMap.put(classTag, clazz);
   }

   public void notifyObject(final long classTag, final long objectId, final long bytes) {
      Class<?> clazz = (Class<?>) classesMap.get(classTag);

      if (clazz != null) // this is not supposed to happen, but just in
      // case I keep this if here
      {
         InventoryDataPoint point = (InventoryDataPoint) maps.get(clazz);
         if (point == null) {
            point = new InventoryDataPoint(clazz);
            maps.put(clazz, point);
         }

         point.bytes += bytes;
         point.instances++;
      }

   }

   public void notifyReference(final long referenceHolder,
                               final long referencedObject,
                               final long classTag,
                               final long index,
                               final long method,
                               final byte referenceType) {
   }

}