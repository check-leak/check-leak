# d-sect

This project has a native (JNI) interface where we expose methods through JVMTI.


It intends to be a java layer on top of JVMTI, where you can inspect memory objects, their references.

The whole point is to be able to write tests that will assert the existence of Objects.


Example:

You need to make sure a certain object is not leaking.


You then do following assertion:

```java
   @Test
   public void itWillLeak() throws Exception {
      doYourProcessing();
      noLeaks(TestClass.class.getName(), 0, 10);
   }
```

ultimately you should get the following result in case of a leak:

```
com.dsect.jvmti.UnexpectedLeak: com.dsect.jvmti.TestClass has 10 elements while we expected 0
References to obj[0]=com.dsect.jvmti.TestClass@52d455b8
!-- arrayRef [Ljava.lang.Object;[0] id=@1330278544
!--!-- FieldReference transient java.lang.Object[] java.util.ArrayList.elementData=OBJ(java.util.ArrayList@519569038)
!--!--!-- StaticFieldReference static java.util.ArrayList com.dsect.jvmti.ExampleTest.elements
!-- arrayRef [Ljava.lang.Object;[0] id=@1870252780
!--!-- Reference inside a method - com.dsect.jvmti.JVMTIInterface::exploreObjectReferences
!--!-- Reference inside a method - com.dsect.jvmti.JVMTIInterface::noLeaks
```


You also have methods to inspect objects, such as getObjects(class), getReferencers(Object), etc..
