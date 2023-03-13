Check-Leak is a project you can use to detect memory leaks. 

It can be used as a library encapsulating JVMTI and providing you way to inspect the existance of objects and their references (why they are still in the heap and not garbage collected).

Or you can use it to inpsect the VM on a remote process, in a lite weight way, by just looking at the GC Histograms of your VM and providing you a nice visualization tool.


## Running as a tool

You can download check-leak-0.9.jar and run:

```shell
java -jar check-leak-0.9.jar --pid <PID> --report <reportoutput> --sleep <interval in milliseconds>
```

Here is an example report, extract from a run over [ActiveMQ Artemis](https://clebertsuconic.github.io).

This will provide you nice [chart views](https://clebertsuconic.github.io/charts/2960.html) about memory consumption for your objects.


## Library Installation

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

Most users will use remove check-leak by simply typing java -jar check-leak.jar. 

However if you require to inspect the objects in the VM, the API might be useful for your.

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
### Extract the native library
```shell
java -jar check-leak.jar install check-leak.so
```

## Releasing

Look at [RELEASING.md](RELEASING.md) for information on how to cut and deploy a release.
