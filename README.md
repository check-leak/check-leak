Check-Leak is a library for detecting memory leaks in Java applications. It utilizes the Java Virtual Machine Tool Interface (JVMTI) to interact directly with the JVM, providing detailed information objects usage.

It can be used to detect and diagnose memory leaks.

## Installation

Check Leak is available on the Central Repository. All you have to do is to define a package dependency:

For Maven:

```xml
<dependency>
  <groupId>io.github.check-leak</groupId>
  <artifactId>check-leak</artifactId>
  <version>0.8</version>
</dependency>
```

For Gradle:

```gradle
dependencies {
  implementation 'io.github.check-leak:check-leak:0.8'
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

## Agent

You can also use check-leak as an agent. The agent will execute a lightweight memory dump just to capture the inventory and print how many objects you have on each class type.

You run a repetitive test and monitor the new allocations in your process.

The agent will keep a max number of Objects and bytes per type, and it prints when that limit reached.

After some time if you still see allocations chances are you have a leak in your system.

### Extract the native library
```shell
java -jar check-leak.jar check-leak.so
```

### Configure your VM

Add the following attributes on the VM settings for your process:

```
-agentpath:check-leak.so -javaagent:check-leak.jar=sleep=20000
```

### You should see an output on your System.out for what's been recently allocated:
```
*******************************************************************************************************************************
Check-Leak Agent
2023-03-02 at 21:33:37 EST
|[Ljava.lang.Object;                                                                                 |        6415680 bytes (+39280)|         6479 instances (+762)|
|[B                                                                                                  |       4617536 bytes (+226664)|       36085 instances (-3123)|
|[I                                                                                                  |        4368960 bytes (+64360)|          1234 instances (+72)|
|java.lang.String                                                                                    |         924552 bytes (+81840)|       38523 instances (+3410)|
|[C                                                                                                  |          971496 bytes (-7560)|           801 instances (+14)|
|java.util.concurrent.ConcurrentHashMap$Node                                                         |         626464 bytes (+24928)|        19577 instances (+779)|
|java.util.HashMap$Node                                                                              |         335232 bytes (+52256)|       10476 instances (+1633)|
|[Lorg.eclipse.jetty.util.TreeTrie$Node;                                                             |          271440 bytes (+9072)|          1885 instances (+63)|
|[Ljava.util.HashMap$Node;                                                                           |         207344 bytes (+15808)|          1961 instances (+61)|
|java.util.LinkedHashMap$Entry                                                                       |          197000 bytes (+3520)|          4925 instances (+88)|
|java.lang.Object                                                                                    |          188832 bytes (+9888)|        11802 instances (+618)|
|[Ljava.util.concurrent.ConcurrentHashMap$Node;                                                      |          185488 bytes (+5776)|           285 instances (+17)|
|java.lang.reflect.Method                                                                            |          183920 bytes (+7040)|          2090 instances (+80)|
|java.util.ArrayList                                                                                 |          80688 bytes (+11016)|         3362 instances (+459)|
|java.util.LinkedHashMap                                                                             |            84448 bytes (+504)|           1508 instances (+9)|
```

### Agent parameters

|Parameter| Default | Description |
|---------|---------|-------------|
|sleep    |60000 | The interval in which we take snapshots.|
|output   | null (meaning System.out) | The name of the file where the inventory is printed |
|down     | -1 (disable) |We update the metrics down once it reaches a bellow this percentage. This is in %. Example if set this to 10, it will update the metric when the new value is bellow 10% of the max it achieved (or previous value) |

## Releasing

Look at [RELEASING.md](RELEASING.md) for information on how to cut and deploy a release.
