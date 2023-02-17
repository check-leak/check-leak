Check-Leak is a library for detecting memory leaks in Java applications. It utilizes the Java Virtual Machine Tool Interface (JVMTI) to interact directly with the JVM, providing detailed information objects usage.

It can be used to detect and diagnose memory leaks.

## Installation

Check Leak is available on the Central Repository. All you have to do is to define a package dependency:

For Maven:

```xml
<dependency>
  <groupId>io.github.check-leak</groupId>
  <artifactId>core</artifactId>
  <version>0.7</version>
</dependency>
```

For Gradle:

```gradle
dependencies {
  implementation 'io.github.check-leak:core:0.7'
}
```

## Basic API

Everything you  need is part of io.github.checkleak.core.CheckLeak.

The most commonly used method is checkLeak.getAllObjects() where you can use JUnit Assertions to validate if they are still around as expected or not.

The following example is also available as part of the [source code](https://github.com/check-leak/check-leak/tree/main/examples/junit-example).

```java
package io.github.checkleak.junitexample;

import io.github.checkleak.core.CheckLeak;
import io.github.checkleak.sample.SomeClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AvoidLeaksTest
{
   @Test
   public void assertOneObject() throws Exception {
      // I am keeping a reference live
      SomeClass someObject = new SomeClass();

      // I am starting the JVMTIInterface API
      CheckLeak checkLeak = new CheckLeak();

      // I'm checking if there are references. On this case I know I should have one object live, so I'm checking for 1
      Assertions.assertEquals(1, checkLeak.getAllObjects(SomeClass.class).length);

      // You can use the exploreObjectReferences to find where the references are (in case they are not expected)
      System.out.println("references to object:" + checkLeak.exploreObjectReferences(10, 10, true, someObject));

      // Now I am clearing the reference
      someObject = null;

      // I'm checking again from JVMTIInterface, if all references are gone. Notice that getAllObjects will force a garbage collection on every call
      Assertions.assertEquals(0, checkLeak.getAllObjects(SomeClass.class).length);
   }
}
```

## Installing the native agent
Before using CheckLeak you need to have access to the native agent. We have provided a maven-plugin that will copy the required library at your location.

You have to also configure the surefire-plugin to allow the --agentpath to work accordingly.

Notice the maven install plugin will copy the appropriate file for your environment. Currently we support MAC and Linux. The library can be compiled on any environment where a GCC compiler is available.

````xml

<!-- you can add this to your pom -->
<properties>
   <check-leak-version>0.7</check-leak-version>
</properties>

<build>
   <plugins>
      <plugin>
         <groupId>io.github.check-leak</groupId>
         <artifactId>checkleak-maven-plugin</artifactId>
         <version>${check-leak-version}</version>
         <executions>
            <execution>
               <phase>generate-sources</phase>
               <id>install-native</id>
               <goals>
                  <goal>install</goal>
               </goals>
               <configuration>
                  <target>${project.basedir}/target/lib</target>
                  <!-- notice this name is totally of your choice. It has to match the name passed to the java argument -->
                  <lib>agent.dll</lib>
               </configuration>
            </execution>
         </executions>
      </plugin>
   </plugins>
</build>

<pluginManagement>
<plugins>
   <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>2.18.1</version>
      <configuration>
         <forkMode>once</forkMode>
         <testFailureIgnore>false</testFailureIgnore>
         <runOrder>alphabetical</runOrder>
         <redirectTestOutputToFile>false</redirectTestOutputToFile>
         
         <!-- it is important to define the --agentpath, with the name and location here matching the one of your choice on the install -->
         <argLine>-agentpath:${project.basedir}/target/lib/agent.dll</argLine>
      </configuration>
   </plugin>
</plugins>
</pluginManagement>

````

## Monitoring a long runnig process

It is also possible to get results on a long running process. 

We provide check-leak-tick as a Java Agent that will start a thread, capture snapshots of what the VM has allocated and show what's recently allocated.

It currently hard coded the tick on every 10 seconds, logging on System.out.

to use this feature you should do the following:

### Extract the native library
```shell
java -jar check-leak-tick.jar check-leak.dll
```

### Configure your VM

Add the following attributes on the VM settings for your process:

```
-agentpath:/Users/clebertsuconic/work/kevin/pub-sub/node1/bin/test.dll
```

### You should see an output on your System.out for what's been recently allocated:
```
*******************************************************************************************************************************
TickAgent tick
|[B|11977328 bytes | 35243 instances|
|[Ljava.lang.Object;|7307888 bytes | 15457 instances|
|[I|3478896 bytes | 1627 instances|
|java.util.HashMap$Node|1180640 bytes | 36895 instances|
|java.lang.String|1072656 bytes | 44694 instances|
|java.util.HashMap|500256 bytes | 10422 instances|
|[Ljava.util.HashMap$Node;|451616 bytes | 3145 instances|
|java.lang.Object|225904 bytes | 14119 instances|
|[J|179120 bytes | 1312 instances|
|io.netty.buffer.PoolSubpage|158904 bytes | 2207 instances|
|java.util.concurrent.locks.ReentrantLock$NonfairSync|84960 bytes | 2655 instances|
|java.util.concurrent.locks.ReentrantLock|42352 bytes | 2647 instances|
|jdk.internal.ref.CleanerImpl$PhantomCleanableRef|16176 bytes | 337 instances|
|java.util.concurrent.ConcurrentLinkedQueue$Node|12960 bytes | 540 instances|
|org.apache.activemq.artemis.core.persistence.impl.journal.OperationContextImpl|12880 bytes | 115 instances|
|org.apache.activemq.artemis.utils.actors.OrderedExecutor|11920 bytes | 298 instances|
|org.apache.qpid.proton.amqp.UnsignedInteger|11504 bytes | 719 instances|
|java.util.concurrent.ConcurrentLinkedQueue|10776 bytes | 449 instances|
|java.lang.ThreadLocal$ThreadLocalMap$Entry|9856 bytes | 308 instances|
|[Ljava.lang.ThreadLocal$ThreadLocalMap$Entry;|6320 bytes | 79 instances|
|java.util.Collections$SetFromMap|4968 bytes | 207 instances| 
|org.apache.activemq.artemis.utils.actors.ProcessorBase$$Lambda$184/0x000000080035e040|4768 bytes | 298 instances|
|java.util.IdentityHashMap|2080 bytes | 52 instances|
|java.lang.ThreadLocal$ThreadLocalMap|1896 bytes | 79 instances| 
|java.util.IdentityHashMap$KeySet|752 bytes | 47 instances|
|org.apache.activemq.artemis.utils.actors.HandlerBase$Counter|672 bytes | 42 instances|
|[Lsun.nio.fs.NativeBuffer;|512 bytes | 16 instances|
|sun.nio.fs.NativeBuffer|512 bytes | 16 instances|
|sun.nio.fs.NativeBuffer$Deallocator|384 bytes | 16 instances|
```

Notice that each time the VM is swiped for the memory the agent is only showing what has increased.

This is an active area of development, and we are looking for ways to improve this. The idea is to swipe the Inventory every few seconds and show the progress of what is leaking.

If you have ideas on how to improve this and make this useful for you, please let us know!


## Releasing

Look at [RELEASING.md](RELEASING.md) for information on how to cut and deploy a release.
