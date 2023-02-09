# check-leak
Check-Leak is a powerful and efficient library for detecting memory leaks in Java applications. It utilizes the Java Virtual Machine Tool Interface (JVMTI) to interact directly with the JVM, providing precise and detailed information about the memory usage of your program. This makes Check-Leak an ideal tool for identifying and resolving memory leaks in Java applications.

# Basic API

The Basic API is defined as part of JVMTIInterface. You simply instantiate org.checkleak.jvmti.JVMTIInterface and work with it.

The most common used method is jvmti.getAllObjects() where you can use JUNIT Assertions to validate if they are still around as expected or not.

The following example is also available as part of the [source code](https://github.com/check-leak/check-leak/tree/main/examples/junit-example).


```java

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

```


# Installing the native agent
Before using JVMTIInterface you need to have access to the native agent. We have provided a maven-plugin that will copy the required library at your location.

You have to also configure the surefire-plugin to allow the --agentpath to work accordingly.

Notice the maven instal plugin will copy the appropriate file for your environment. Currently we support MAC and Linux. It is possible to compile the library on any environent where a gcc compiler is available.

````xml

<!-- you can add this to your pom -->
<properties>
   <check-leak-version>0.4</check-leak-version>
</properties>

<build>
   <plugins>
      <plugin>
         <groupId>org.check-leak</groupId>
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