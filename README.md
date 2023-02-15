Check-Leak is a library for detecting memory leaks in Java applications. It utilizes the Java Virtual Machine Tool Interface (JVMTI) to interact directly with the JVM, providing detailed information objects usage.

It can be used to detect and diagnose memory leaks.

## Installation

Check Leak is available on the Central Repository. All you have to do is to define a package dependency:

For maven:
```xml
<dependency>
  <groupId>io.github.check-leak</groupId>
  <artifactId>core</artifactId>
  <version>0.7</version>
</dependency>
```

For Gradle:
```shell
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


## Releasing

Look at [RELEASING.md](RELEASING.md) for information on how to cut and deploy a release.
