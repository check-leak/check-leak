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

package org.checkleak.junitexample;

import org.checkleak.jvmti.JVMTIInterface;
import org.checkleak.sample.SomeClass;
import org.junit.Assert;
import org.junit.Test;

public class AvoidLeaksTest
{
    @Test
    public void assertOneObject() throws Exception {
        // I am keeping a reference live
        SomeClass someObject = new SomeClass();

        // I am starting the JVMTIInterface API
        JVMTIInterface jvmtiInterface = new JVMTIInterface();

        // I'm checking if there are references. On this case I know I should have one object live, so I'm checking for 1
        Assert.assertEquals(1, jvmtiInterface.getAllObjects(SomeClass.class).length);

        // You can use the exploreObjectReferences to find where the references are (in case they are not expected)
        System.out.println("references to object:" + jvmtiInterface.exploreObjectReferences(10, 10, true, someObject));

        // Now I am clearing the reference
        someObject = null;

        // I'm checking again from JVMTIInterface, if all references are gone. Notice that getAllObjects will force a garbage collection on every call
        Assert.assertEquals(0, jvmtiInterface.getAllObjects(SomeClass.class).length);
    }
}
