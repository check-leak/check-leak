/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007-2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package io.github.checkleak.core;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** This is the main point of use on the API. It is the class reponsible to interface with JVMTI.
 *  The most common methods are getAllObjects and exploreObjectReferences */
public class CheckLeak {

   private static boolean isLoaded = true;

   static {
      try {
         CheckLeak tst = new CheckLeak();
         // Calling the blank method, just to make sure the library is loaded
         tst.blank();
      } catch (Throwable e) {
         try {
            System.loadLibrary("checkleak");
         } catch (Throwable e2) {
            e2.printStackTrace();
            isLoaded = false;
         }
      }
   }

   /**
    * Returns true if -agentlib:JBossProfiler was configured properly
    * @return true if loaded
    */
   public static boolean isLoaded() {
      return isLoaded;
   }

   /**
    * This is a blank method, intended just to verify if the library is loaded correctly
    */
   public native void blank();

   /**
    * Force a GC. This method doesn't use System.gc. If JVMTI is enabled this
    * will really cause a FullGC by calling a JVMTI function.
    */
   public native void forceGC();

   /** There might be more than one class with a matching name (example multiple class loaders)
    * @param className The Classname.
    * @return All The classes matching the classname on every classLoader.
    * */
   public Class<?>[] getAllClasses(final String className) {
      Class<?> classes[] = getLoadedClasses();

      ArrayList<Class<?>> foundClasses = new ArrayList<Class<?>>();

      for (Class<?> clazz : classes) {
         if (clazz.getName().equals(className)) {
            foundClasses.add(clazz);
         }
      }

      return foundClasses.toArray(new Class<?>[foundClasses.size()]);
   }

   /**
    * returns the first class found with a given name
    * @param className The name of the class
    * @return the first class found on the heap matching className
    */
   public Class<?> getClassByName(final String className) {
      Class<?> classes[] = getLoadedClasses();

      for (Class<?> classe : classes) {
         if (classe.getName().equals(className)) {
            return classe;
         }
      }

      return null;
   }

   /**
    * Will release internal tags used by previous methods. All the navigations
    * through JVMTI are done through tagging. Calling this method will release
    * any tagging done by previous methods.
    */
   public native void releaseTags();

   /**
    *
    * Internal method. used by notifyOnReferences.
    * @param notifyOnClasses should call the callback on classes.
    * @param temporaryFileReferences obvious
    * @param temporaryFileObjects obvious
    * @param callback the callback to be used
    *
    */
   protected native void notifyInventory(boolean notifyOnClasses,
                                         String temporaryFileReferences,
                                         String temporaryFileObjects,
                                         JVMTICallBack callback);

   /** internal method. It will callback for everything on the heap.
    * A temporary file is required as we can't use Java Objects from the native side. while we are inspecting things. We will execute the callback for everything in the heap before this executions.
    * @param temporaryFile a temporary file that will be used in the process.
    * @param callback The native implementation will call this callback for everything used in the heap.
    * */
   public void notifyOnReferences(final String temporaryFile, final JVMTICallBack callback) {
      notifyInventory(true, temporaryFile, null, callback);
   }

   /**
    * Will get all the objects holding references to these objects passed by
    * parameter. This method is going to release tags, be careful if you are on
    * the middle of navigations.
    *
    * @param objects a list of objects you are looking for referencers
    *
    * @return a lit of objects that are referencing all the objects sent as parameters
    */
   public native Object[] getReferenceHolders(Object... objects);

   public native Class<?>[] getLoadedClasses();

   /**
    * Will return all methods of a give class. This will change tags, be
    * careful if you are on the middle of navigations.
    *
    * @param clazz The class we want object instances.
    *
    * @return All the objects that are instances of clazz.
    */
   public native Object[] getAllObjects(Class<?> clazz);

   /**
    * Will return the tag of an object
    */
   private native long getTagOnObject(Object obj);

   /**
    * Will return the object on a tag
    */
   private native Object getObjectOnTag(long tag);

   JVMTIFieldsMetadata metadata = new JVMTIFieldsMetadata();

   /**
    * Returns the field represted by the FieldId. This is used on field
    * relationships according to the rule determined by JVMTI documentation.
    */
   private Field getObjectField(Class<?> clazz, int fieldId) {

      Field[] fields = metadata.getFields(clazz);
      return metadata.getFields(clazz)[fieldId];
   }

   private native String getMethodName(long methodId);

   private native String getMethodSignature(long methodId);

   private native Class getMethodClass(long methodId);

   public Object[] getAllObjects(final String clazz) {
      ArrayList list = new ArrayList();

      Class[] classes = getLoadedClasses();
      for (Class classe : classes) {
         if (classe.getName().equals(clazz)) {
            Object objs[] = this.getAllObjects(classe);
            for (Object obj : objs) {
               list.add(obj);
            }
         }
      }

      return list.toArray();
   }

   private String convertToString(final Object obj, final boolean callToString) {

      String returnValue = null;
      try {
         if (obj == null) {
            returnValue = "null";
         } else {
            if (callToString) {
               returnValue = "TOSTRING(" + obj.toString() + "), class=" + obj.getClass().getName();
            } else {
               if (obj instanceof Class) {
                  returnValue = "CLASS(" + obj.toString() + "), identifyHashCode=" + System.identityHashCode(obj);
               } else {
                  returnValue = "OBJ(" + obj.getClass().getName() + "@" + System.identityHashCode(obj) + ")";
               }
            }
         }
      } catch (Throwable e) {
         return obj.getClass().getName() + " toString had an Exception ";
      }

      if (returnValue.length() > 200) {
         return "OBJ(" + obj.getClass().getName() + "@" + System.identityHashCode(obj) + ")";
      } else {
         return returnValue;
      }
   }

   /*
    * Explore references recursevely
    */
   private void exploreObject(final PrintWriter out,
                              final Object source,
                              final int currentLevel,
                              final int maxLevel,
                              final boolean useToString,
                              final boolean weakAndSoft,
                              final Map mapDataPoints,
                              final HashSet alreadyExplored) {
      String level = null;
      {
         StringBuffer levelStr = new StringBuffer();
         for (int i = 0; i <= currentLevel; i++) {
            levelStr.append("!--");
         }
         level = levelStr.toString();
      }

      if (maxLevel >= 0 && currentLevel >= maxLevel) {
         out.println(level + "MaxLevel");
         return;
      }
      Integer index = new Integer(System.identityHashCode(source));

      if (alreadyExplored.contains(index)) {
         if (source instanceof Class) {
            out.println(level + " object instanceOf " + source + "@" + index + " was already described before on this report");
         } else {
            out.println(level + " object instanceOf " + source.getClass() + "@" + index + " was already described before on this report");
         }
         return;
      }

      alreadyExplored.add(index);

      Long sourceTag = new Long(getTagOnObject(source));
      ArrayList listPoints = (ArrayList) mapDataPoints.get(sourceTag);
      if (listPoints == null) {
         return;
      }

      Iterator iter = listPoints.iterator();

      while (iter.hasNext()) {
         ReferenceDataPoint point = (ReferenceDataPoint) iter.next();

         Object nextReference = treatReference(level, out, point, useToString);

         if (nextReference != null && !weakAndSoft) {
            if (nextReference instanceof WeakReference || nextReference instanceof SoftReference) {
               nextReference = null;
            }
         }

         if (nextReference != null) {
            exploreObject(out, nextReference, currentLevel + 1, maxLevel, useToString, weakAndSoft, mapDataPoints, alreadyExplored);
         }
      }

   }

   /* Treat a reference sending it tout the PrintWriter output */
   private Object treatReference(final String level,
                                final PrintWriter out,
                                final ReferenceDataPoint point,
                                final boolean useToString) {
      Object referenceHolder = null;
      if (point.getReferenceHolder() == 0 || point.getReferenceHolder() == -1) {
         referenceHolder = null;
      } else {
         referenceHolder = getObjectOnTag(point.getReferenceHolder());
      }
      Object nextReference = null;
      switch (point.getReferenceType()) {
         case JVMTITypes.JVMTI_REFERENCE_INSTANCE:
            ;// Reference from an object to its class.
            out.println(level + "InstanceOfReference:ToString=" + convertToString(referenceHolder, useToString));

            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_FIELD:// Reference from an object to the
            // value of one of its instance
            // fields. For references of this kind the referrer_index parameter
            // to
            // the jvmtiObjectReferenceCallback is the index of the the instance
            // field. The index is based on the order of all the object's
            // fields.
            // This includes all fields of the directly declared static and
            // instance fields in the class, and includes all fields (both
            // public
            // and private) fields declared in superclasses and superinterfaces.
            // The index is thus calculated by summing the index of field in the
            // directly declared class (see GetClassFields), with the total
            // number
            // of fields (both public and private) declared in all superclasses
            // and superinterfaces. The index starts at zero.
         {

            String fieldName = null;

            if (referenceHolder == null) {
               fieldName = "Reference GONE";
            } else {
               Class clazz = referenceHolder.getClass();
               Field field = getObjectField(clazz, (int) point.getIndex());
               if (field == null) {
                  fieldName = "UndefinedField@" + referenceHolder;
               } else {
                  fieldName = "name='" + field.getName() + "'::=" + field;
               }
            }
            out.println(level + " FieldReference " + fieldName + " on object " + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         }
         case JVMTITypes.JVMTI_REFERENCE_ARRAY_ELEMENT:// Reference from an array
            // to one of its
            // elements. For
            // references of this kind the referrer_index parameter to the
            // jvmtiObjectReferenceCallback is the array index.
            if (referenceHolder == null) {
               out.println(level + " arrayRef Position " + point.getIndex() + " is gone");
            } else {
               out.println(level + " arrayRef " + referenceHolder.getClass().getName() + "[" + point.getIndex() + "] id=@" + System.identityHashCode(referenceHolder));
            }
            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_CLASS_LOADER:// Reference from a class
            // to its class loader.
            out.println(level + "ClassLoaderReference @ " + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_SIGNERS:// Reference from a class to its
            // signers array.
            out.println(level + "ReferenceSigner@" + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_PROTECTION_DOMAIN:// Reference from a
            // class to its
            // protection
            // domain.
            out.println(level + "ProtectionDomain@" + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_INTERFACE:// Reference from a class to
            // one of its interfaces.
            out.println(level + "ReferenceInterface@" + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_STATIC_FIELD:// Reference from a class
            // to the value of one
            // of its static
            // fields. For references of this kind the referrer_index
            // parameter to the jvmtiObjectReferenceCallback is the index
            // of the static field. The index is based on the order of the
            // directly declared static and instance fields in the class
            // (not inherited fields), starting at zero. See
            // GetClassFields.
         {
            Class clazz = (Class) referenceHolder;
            Field field = getObjectField(clazz, (int) point.getIndex());
            String fieldName = null;
            if (field == null) {
               fieldName = "UndefinedField@" + referenceHolder;
            } else {
               fieldName = field.toString();
            }
            out.println(level + " StaticFieldReference " + fieldName);
            nextReference = null;
            break;
         }
         case JVMTITypes.JVMTI_REFERENCE_CONSTANT_POOL:// Reference from a class
            // to a resolved entry
            // in the constant
            // pool. For references of this kind the referrer_index
            // parameter to the jvmtiObjectReferenceCallback is the index
            // into constant pool table of the class, starting at 1. See
            // The Constant Pool in the Java Virtual Machine
            // Specification.
            out.println(level + "ReferenceInterface@" + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTITypes.ROOT_REFERENCE:
            out.println(level + "Root");
            nextReference = null;
            break;
         case JVMTITypes.THREAD_REFERENCE:

            Class methodClass = getMethodClass(point.getMethod());
            if (methodClass != null) {
               String className = null;
               if (methodClass != null) {
                  className = methodClass.getName();
               }

               String methodName = getMethodName(point.getMethod());
               out.println(level + " Reference inside a method - " + className + "::" + methodName);
            }
            nextReference = null;
            break;
         default:
            System.out.println("unexpected reference " + point);
      }
      return nextReference;
   }

   /**
    * @return a matrix that can be used for navigations through the API.
    * @throws IOException in case the temporary file generation failed.
    */
   public Map<Long, List<ReferenceDataPoint>> createIndexMatrix() throws IOException {
      releaseTags();
      final HashMap<Long, List<ReferenceDataPoint>> referencesMap = new HashMap<Long, List<ReferenceDataPoint>>();
      File tmpFile = File.createTempFile("tmpRefs", ".tmp");
      notifyOnReferences(tmpFile.getAbsolutePath(), new JVMTICallBack() {
         int count = 0;

         public void notifyReference(final long referenceHolder,
                                     final long referencedObject,
                                     final long classTag,
                                     final long index,
                                     final long method,
                                     final byte referenceType) {
            ReferenceDataPoint dataPoint = new ReferenceDataPoint(referenceHolder, referencedObject, classTag, index, method, referenceType);
            Long indexLong = new Long(referencedObject);
            List<ReferenceDataPoint> arrayList = (ArrayList<ReferenceDataPoint>) referencesMap.get(indexLong);
            if (arrayList == null) {
               arrayList = new ArrayList<ReferenceDataPoint>();
               referencesMap.put(indexLong, arrayList);
            }
            arrayList.add(dataPoint);
         }

         public void notifyClass(final long classTag, final Class clazz) {
         }

         public void notifyObject(final long classTag, final long objectId, final long bytes) {
         }
      });

      tmpFile.delete();

      return referencesMap;
   }

   public String exploreClassReferences(final String className,
                                        final int maxLevel,
                                        final boolean solveReferencesOnClasses,
                                        final boolean solveReferencesOnClassLoaders,
                                        final boolean useToString,
                                        final boolean weakAndSoft,
                                        final boolean printObjects) {
      forceGC();
      if (!solveReferencesOnClasses && !solveReferencesOnClassLoaders && !printObjects) {
         return "<b> you have to select at least solveReferences || solveClassLoaders || printObjects </b>";
      }

      Map referencesMap = null;
      try {
         referencesMap = createIndexMatrix();
      } catch (Exception e) {
         CharArrayWriter charArray = new CharArrayWriter();
         PrintWriter out = new PrintWriter(charArray);
         e.printStackTrace(out);
         return charArray.toString();
      }

      try {
         CharArrayWriter charArray = new CharArrayWriter();
         PrintWriter out = new PrintWriter(charArray);

         out.println(exploreClassReferences(className, maxLevel, solveReferencesOnClasses, solveReferencesOnClassLoaders, useToString, weakAndSoft, referencesMap));

         if (printObjects) {
            releaseTags();
            Class classes[] = getLoadedClasses();

            for (Class clazz : classes) {
               if (clazz.getName().equals(className)) {
                  Object[] objs = this.getAllObjects(clazz);

                  if (objs.length != 0) {
                     out.println("Instances of:" + clazz.getName() + " ClassLoader=" + clazz.getClassLoader());
                     for (int countOBJ = 0; countOBJ < objs.length; countOBJ++) {
                        out.println(clazz.getName() + "[" + countOBJ + "]=" + objs[countOBJ]);
                     }
                  }
               }
            }
         }

         out.flush();

         return charArray.toString();
      } finally {
         releaseTags();
         metadata.clear();
      }
   }

   /**
    * Used to navigate and explore references of className, while using an index.
    * @param className the className
    * @param maxLevel how many levels to be recursive for the references
    * @param solveReferencesOnClasses solve class references
    * @param solveReferencesOnClassLoaders  solve classLoaders references
    * @param useToString print toSring
    * @param weakAndSoft should explore weak and soft references as well
    * @param referencesMap the index created from navigation
    *
    * @return the report generated
    *
    */
   public String exploreClassReferences(final String className,
                                        final int maxLevel,
                                        final boolean solveReferencesOnClasses,
                                        final boolean solveReferencesOnClassLoaders,
                                        final boolean useToString,
                                        final boolean weakAndSoft,
                                        final Map referencesMap) {
      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      try {

         Class[] loadClasses = getLoadedClasses();

         for (Class loadClasse : loadClasses) {
            if (loadClasse.getName().equals(className)) {
               out.println("References to " + loadClasse);
               if (solveReferencesOnClasses) {
                  exploreObject(out, loadClasse, 0, maxLevel, useToString, weakAndSoft, referencesMap, new HashSet());
               }
               if (solveReferencesOnClassLoaders) {
                  if (loadClasse.getClassLoader() != null) {
                     out.println("references to its classloader " + loadClasse.getClassLoader());
                     exploreObject(out, loadClasse.getClassLoader(), 0, maxLevel, useToString, weakAndSoft, referencesMap, new HashSet());
                  }
               }
               out.println();

            }
         }

         loadClasses = null;

         return charArray.toString();
      } catch (Exception e) {
         charArray = new CharArrayWriter();
         out = new PrintWriter(charArray);
         e.printStackTrace(out);
         return charArray.toString();
      }
   }

   /**
    * Show the reference holders tree of an object. This method is also exposed
    * through MBean.
    *
    * @param className the class being explored
    * @param maxLevel max recursion on the report
    * @param maxObjects the max many of objects to reach from the class
    * @param useToString print toString
    * @return the report generated
    * @throws Exception any exception that happened on this process
    */
   public String exploreObjectReferences(final String className,
                                         final int maxLevel,
                                         final int maxObjects,
                                         final boolean useToString) throws Exception {
      forceGC();

      Object obj[] = this.getAllObjects(className);

      return exploreObjectReferences(maxLevel, maxObjects, useToString, obj);
   }

   public String findRoots(int maxLevel, boolean useToString, Object... obj) throws Exception {
      Map referencesMap = null;
      referencesMap = createIndexMatrix();

      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      try {
         for (int i = 0; i < Math.min(50, obj.length); i++) {
            CharArrayWriter charLeaf = new CharArrayWriter();
            PrintWriter outLeaf = new PrintWriter(charArray);
            if (findRootRecursevely(outLeaf, obj[i], 0, maxLevel, useToString, false, referencesMap, new HashSet())) {
               out.print(charLeaf.toString());

            }
         }
         return charArray.toString();
      } finally {
         referencesMap.clear();
         releaseTags();
         metadata.clear();
      }
   }

   /**
    * Explore references recursevely
    */
   private boolean findRootRecursevely(final PrintWriter out,
                                       final Object source,
                                       final int currentLevel,
                                       final int maxLevel,
                                       final boolean useToString,
                                       final boolean weakAndSoft,
                                       final Map mapDataPoints,
                                       final HashSet alreadyExplored) {
      String level = null;
      {
         StringBuffer levelStr = new StringBuffer();
         for (int i = 0; i <= currentLevel; i++) {
            levelStr.append("!--");
         }
         level = levelStr.toString();
      }

      if (maxLevel >= 0 && currentLevel >= maxLevel) {
         return false;
      }
      Integer index = new Integer(System.identityHashCode(source));

      if (alreadyExplored.contains(index)) {
         return false;
      }

      alreadyExplored.add(index);

      Long sourceTag = new Long(getTagOnObject(source));
      ArrayList listPoints = (ArrayList) mapDataPoints.get(sourceTag);
      if (listPoints == null) {
         return false;
      }

      Iterator iter = listPoints.iterator();

      while (iter.hasNext()) {
         ReferenceDataPoint point = (ReferenceDataPoint) iter.next();

         Object referenceHolder = null;
         if (point.getReferenceHolder() == 0 || point.getReferenceHolder() == -1) {
            referenceHolder = null;
         } else {
            referenceHolder = getObjectOnTag(point.getReferenceHolder());
         }
         Object nextReference = null;
         switch (point.getReferenceType()) {
            case JVMTITypes.JVMTI_REFERENCE_INSTANCE:
               ;// Reference from an object to its class.
               out.println(level + "InstanceOfReference:ToString=" + convertToString(referenceHolder, useToString));

               nextReference = referenceHolder;
               break;
            case JVMTITypes.JVMTI_REFERENCE_FIELD:// Reference from an object to the
               // value of one of its instance
               // fields. For references of this kind the referrer_index parameter
               // to
               // the jvmtiObjectReferenceCallback is the index of the the instance
               // field. The index is based on the order of all the object's
               // fields.
               // This includes all fields of the directly declared static and
               // instance fields in the class, and includes all fields (both
               // public
               // and private) fields declared in superclasses and superinterfaces.
               // The index is thus calculated by summing the index of field in the
               // directly declared class (see GetClassFields), with the total
               // number
               // of fields (both public and private) declared in all superclasses
               // and superinterfaces. The index starts at zero.
            {

               String fieldName = null;

               if (referenceHolder == null) {
                  fieldName = "Reference GONE";
               } else {
                  Class clazz = referenceHolder.getClass();
                  Field field = getObjectField(clazz, (int) point.getIndex());
                  if (field == null) {
                     fieldName = "UndefinedField@" + referenceHolder;
                  } else {
                     fieldName = field.toString();
                  }
               }
               out.println(level + " FieldReference " + fieldName + "=" + convertToString(referenceHolder, useToString));
               nextReference = referenceHolder;
               break;
            }
            case JVMTITypes.JVMTI_REFERENCE_ARRAY_ELEMENT:// Reference from an array
               // to one of its
               // elements. For
               // references of this kind the referrer_index parameter to the
               // jvmtiObjectReferenceCallback is the array index.
               if (referenceHolder == null) {
                  out.println(level + " arrayRef Position " + point.getIndex() + " is gone");
               } else {
                  out.println(level + " arrayRef " + referenceHolder.getClass().getName() + "[" + point.getIndex() + "] id=@" + System.identityHashCode(referenceHolder));
               }
               nextReference = referenceHolder;
               break;
            case JVMTITypes.JVMTI_REFERENCE_CLASS_LOADER:// Reference from a class
               // to its class loader.
               out.println(level + "ClassLoaderReference @ " + convertToString(referenceHolder, useToString));
               nextReference = referenceHolder;
               break;
            case JVMTITypes.JVMTI_REFERENCE_SIGNERS:// Reference from a class to its
               // signers array.
               out.println(level + "ReferenceSigner@" + convertToString(referenceHolder, useToString));
               nextReference = referenceHolder;
               break;
            case JVMTITypes.JVMTI_REFERENCE_PROTECTION_DOMAIN:// Reference from a
               // class to its
               // protection
               // domain.
               out.println(level + "ProtectionDomain@" + convertToString(referenceHolder, useToString));
               nextReference = referenceHolder;
               break;
            case JVMTITypes.JVMTI_REFERENCE_INTERFACE:// Reference from a class to
               // one of its interfaces.
               out.println(level + "ReferenceInterface@" + convertToString(referenceHolder, useToString));
               nextReference = referenceHolder;
               break;
            case JVMTITypes.JVMTI_REFERENCE_STATIC_FIELD:// Reference from a class
               // to the value of one
               // of its static
               // fields. For references of this kind the referrer_index
               // parameter to the jvmtiObjectReferenceCallback is the index
               // of the static field. The index is based on the order of the
               // directly declared static and instance fields in the class
               // (not inherited fields), starting at zero. See
               // GetClassFields.
            {
               Class clazz = (Class) referenceHolder;
               Field field = getObjectField(clazz, (int) point.getIndex());
               String fieldName = null;
               if (field == null) {
                  fieldName = "UndefinedField@" + referenceHolder;
               } else {
                  fieldName = field.toString();
               }
               out.println(level + " StaticFieldReference " + fieldName);

               nextReference = null;
               if (field != null && field.getDeclaringClass().getName().equals("lang.ref.Finalizer")) {
                  break;
               }
               return true;
            }
            case JVMTITypes.JVMTI_REFERENCE_CONSTANT_POOL:// Reference from a class
               // to a resolved entry
               // in the constant
               // pool. For references of this kind the referrer_index
               // parameter to the jvmtiObjectReferenceCallback is the index
               // into constant pool table of the class, starting at 1. See
               // The Constant Pool in the Java Virtual Machine
               // Specification.
               out.println(level + "ReferenceInterface@" + convertToString(referenceHolder, useToString));
               nextReference = referenceHolder;
               break;
            case JVMTITypes.ROOT_REFERENCE:
               out.println(level + "Root");
               nextReference = null;
               return true;
            case JVMTITypes.THREAD_REFERENCE:

               Class methodClass = getMethodClass(point.getMethod());
               String className;
               if (methodClass != null) {
                  className = null;
                  if (methodClass != null) {
                     className = methodClass.getName();
                  }

                  String methodName = getMethodName(point.getMethod());
                  out.println(level + " Reference inside a method - " + className + "::" + methodName);
               }
               nextReference = null;

               if (methodClass != null && !methodClass.equals(CheckLeak.class)) {
                  return true;
               } else {
                  break;
               }
            default:
               System.out.println("unexpected reference " + point);
         }

         if (!weakAndSoft && (nextReference instanceof WeakReference || nextReference instanceof SoftReference)) {
            nextReference = null;
         }

         if (nextReference != null) {
            return findRootRecursevely(out, nextReference, currentLevel + 1, maxLevel, useToString, weakAndSoft, mapDataPoints, alreadyExplored);
         } else {
            return true;
         }
      }

      return true;

   }

   public String exploreObjectReferences(int maxLevel, int maxObjects, boolean useToString, Object... obj) throws Exception {
      System.out.println("Obj.length = " + obj.length);

      Map referencesMap = null;
      referencesMap = createIndexMatrix();

      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      for (int i = 0; i < Math.min(maxObjects, obj.length); i++) {
         out.println("*******************************************************************************************************************************");
         out.println("References to obj[" + i + "]=" + convertToString(obj[i], useToString));
         out.print(exploreObjectReferences(referencesMap, obj[i], maxLevel, useToString));
      }

      releaseTags();
      referencesMap.clear();
      referencesMap = null; // giving a hand to GC


      out.println("Summary of all the refernce holders:");
      Object[] holders = getReferenceHolders(obj);
      for (Object ob : holders) {
         out.println("-> "  + convertToString(ob, useToString));
      }

      return charArray.toString();
   }

   /**
    * Show the reference holders tree of an object. This returns a report you
    * can visualize through MBean.
    *
    * @param referencesMap the map generated from the HEAP
    * @param thatObject it will explore references on this
    * @param maxLevel max recursion used for the report
    * @param useToString object.toString will be used on the report
    * @return a generated report
    *
    */
   public String exploreObjectReferences(final Map referencesMap,
                                         final Object thatObject,
                                         final int maxLevel,
                                         final boolean useToString) {
      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      try {
         exploreObject(out, thatObject, 0, maxLevel, useToString, false, referencesMap, new HashSet());
         return charArray.toString();
      } catch (Exception e) {
         charArray = new CharArrayWriter();
         out = new PrintWriter(charArray);
         e.printStackTrace(out);
         return charArray.toString();
      }
   }

   /**
    * it will return a WeakHashMap summarizing everything in the memory.
    * @return a produced inventory that can be used for navigations.
    * @throws IOException while generating the tmp file used on the navigation
    */
   public synchronized Map<Class<?>, InventoryDataPoint> produceInventory() throws IOException {
      forceGC();
      JVMTICapture callBack = new JVMTICapture();
      File tmpFileObjects = File.createTempFile("delete-me", ".objects");
      try {
         notifyInventory(true, null, tmpFileObjects.getAbsolutePath(), callBack);
      } finally {
         if (tmpFileObjects.exists()) {
            try {
               tmpFileObjects.delete();
            } catch (Exception ignored) {
            }
         }
      }

      return callBack.maps;
   }
}
