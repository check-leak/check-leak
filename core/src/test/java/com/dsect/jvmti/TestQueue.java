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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of a Queue
 * <p>
 * Completely non blocking between adding to queue and delivering to consumers.
 */
public class TestQueue implements SomeInterface {


   public void doSomething() {
      // empty
   }

   protected static final int CRITICAL_PATHS = 5;
   protected static final int CRITICAL_PATH_ADD_TAIL = 0;
   protected static final int CRITICAL_PATH_ADD_HEAD = 1;
   protected static final int CRITICAL_DELIVER = 2;
   protected static final int CRITICAL_CONSUMER = 3;
   protected static final int CRITICAL_CHECK_DEPAGE = 4;

   private static final AtomicLongFieldUpdater consumerRemovedTimestampUpdater = AtomicLongFieldUpdater.newUpdater(TestQueue.class, "consumerRemovedTimestamp");

   public static final int REDISTRIBUTOR_BATCH_SIZE = 100;

   public static final int NUM_PRIORITIES = 10;

   public static final int MAX_DELIVERIES_IN_LOOP = 1000;

   public static final int CHECK_QUEUE_SIZE_PERIOD = 1000;

   /**
    * If The system gets slow for any reason, this is the maximum time a Delivery or
    * or depage executor should be hanging on
    */
   public static final int DELIVERY_TIMEOUT = 1000;

   private static final int FLUSH_TIMEOUT = 10000;

   public static final int DEFAULT_FLUSH_LIMIT = 500;

   private final long id = 3;

   private final String name = null;

   private String user;

   private final boolean propertyDurable = false;

   private final boolean temporary = false;

   private final boolean autoCreated = false;

   private volatile boolean queueDestroyed = false;

   private volatile boolean printErrorExpiring = false;

   // The quantity of pagedReferences on messageReferences priority list
   private final AtomicInteger pagedReferences = new AtomicInteger(0);

   // The estimate of memory being consumed by this queue. Used to calculate instances of messages to depage
   private final AtomicInteger queueMemorySize = new AtomicInteger(0);

   private AtomicLong messagesAdded = new AtomicLong(0);

   private AtomicLong messagesAcknowledged = new AtomicLong(0);

   private AtomicLong ackAttempts = new AtomicLong(0);

   private AtomicLong messagesExpired = new AtomicLong(0);

   private AtomicLong messagesKilled = new AtomicLong(0);

   private AtomicLong messagesReplaced = new AtomicLong(0);

   private boolean paused;

   private long pauseStatusRecord = -1;

   private static final int MAX_SCHEDULED_RUNNERS = 1;
   private static final int MAX_DEPAGE_NUM = MAX_DELIVERIES_IN_LOOP * MAX_SCHEDULED_RUNNERS;

   // for that we keep a counter of scheduled instances
   private final AtomicInteger scheduledRunners = new AtomicInteger(0);

   //This lock is used to prevent deadlocks between direct and async deliveries
   private final ReentrantLock deliverLock = new ReentrantLock();

   private final ReentrantLock depageLock = new ReentrantLock();

   private volatile boolean depagePending = false;


   private final TestClass myTestField;
   public TestQueue(TestClass myobject) {
      this.myTestField = myobject;
   }

   private volatile long consumerRemovedTimestamp = -1;

}


