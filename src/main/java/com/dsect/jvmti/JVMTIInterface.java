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

package com.dsect.jvmti;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.WeakHashMap;

/**
 * @author Clebert Suconic
 */
public class JVMTIInterface
{
   private static boolean isLoaded = true;
   static
   {
      try
      {
         JVMTIInterface tst = new JVMTIInterface();
         // if this works, the profiler is already loaded
         tst.forceGC();
      }
      catch (Throwable e)
      {
         try
         {
            System.loadLibrary("JBossProfiler");
         }
         catch (Throwable e2)
         {
            isLoaded = false;
            System.out.println("The DLL/SO couldn't be loaded, you won't be able to use any JVMTIInterface feature");
         }
      }
   }

   /**
    * Force a GC. This method doesn't use System.gc. If JVMTI is enabled this
    * will really cause a FullGC by calling a JVMTI function.
    */
   public native void forceGC();

   public native void startMeasure(String directory, String prefix, String suffix);

   public native void stopMeasure();

   /** returns the first class found with a given name */
   public Class<?>[] getAllClassMachingName(final String className)
   {
      Class<?> classes[] = getLoadedClasses();

      ArrayList<Class<?>> foundClasses = new ArrayList<Class<?>>();

      for (Class<?> clazz : classes)
      {
         if (clazz.getName().equals(className))
         {
            foundClasses.add(clazz);
         }
      }

      return foundClasses.toArray(new Class<?>[foundClasses.size()]);
   }

   public Class<?> getClassByName(final String className)
   {
      Class<?> classes[] = getLoadedClasses();

      for (Class<?> classe : classes)
      {
         if (classe.getName().equals(className))
         {
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
    * This method will keep every object tagged for later usage. This method is
    * going to tag objects.
    */
   protected native void notifyInventory(boolean notifyOnClasses, String temporaryFileReferences,
         String temporaryFileObjects, JVMTICallBack callback);

   public void notifyOnReferences(final String temporaryFile, final JVMTICallBack callback)
   {
      notifyInventory(true, temporaryFile, null, callback);
   }

   /**
    * Will get all the objects holding references to these objects passed by
    * parameter. This method is going to release tags, be careful if you are on
    * the middle of navigations.
    */
   public native Object[] getReferenceHolders(Object[] objects);

   public native Class<?>[] getLoadedClasses();

   /**
    * Will return all methods of a give class. This will change tags, be
    * careful if you are on the middle of navigations.
    */
   public native Object[] getAllObjects(Class<?> clazz);

   /** Will return the tag of an object */
   public native long getTagOnObject(Object obj);

   /** Will return the object on a tag */
   public native Object getObjectOnTag(long tag);

   private native boolean internalIsConfiguredProperly();

   /** Returns true if -agentlib:JBossProfiler was configured properly */
   public boolean isActive()
   {
      if (!isLoaded)
      {
         return false;
      }
      else
      {
         return internalIsConfiguredProperly();
      }
   }

   /**
    * Returns the field represted by the FieldId. This is used on field
    * relationships according to the rule determined by JVMTI documentation.
    */
   public Field getObjectField(Class<?> clazz, int fieldId)
   {
      ArrayList<Class<?>> list = new ArrayList<Class<?>>();
      list.add(clazz);
      while ((clazz = clazz.getSuperclass()) != null)
      {
         list.add(clazz);
      }

      for (int i = list.size() - 1; i >= 0; i--)
      {
         Field fields[] = ((Class)list.get(i)).getDeclaredFields();
         if (fieldId < fields.length)
         {
            return fields[fieldId];
         }
         fieldId -= fields.length;
      }
      return null;
   }

   public native String getMethodName(long methodId);

   public native String getMethodSignature(long methodId);

   public native Class getMethodClass(long methodId);

   protected static native void heapSnapshot(String classesFileName, String referencesFileName, String objectsFileName);

   /**
    * Will call {@link JVMTIInterface.heapSnapshot(String,String,String)}
    * passing "_classes, _references, _objects in the name of the files
    */
   public void heapSnapshot(final String basicFileName, final String suffix)
   {
      forceGC();
      heapSnapshot(basicFileName +
            "_classes" + "." + suffix, basicFileName +
            "_references" + "." + suffix, basicFileName +
            "_objects" + "." + suffix);
   }

   /**
    * Return every single object on a give class by its name. This method will
    * look for every single class with this name, and if more than one
    * classLoader is loading a class with this name, this method will return
    * objects for all the respective classes. For example if you look for a
    * Structs Action Form, this will return every ActionForm defined on the
    * current JVM.
    */
   public Object[] getAllObjects(final String clazz)
   {
      ArrayList list = new ArrayList();

      Class[] classes = getLoadedClasses();
      for (Class classe : classes)
      {
         if (classe.getName().equals(clazz))
         {
            Object objs[] = this.getAllObjects(classe);
            for (Object obj : objs)
            {
               list.add(obj);
            }
         }
      }

      return list.toArray();
   }

   static class ClassSorterByClassLoader implements Comparator
   {

      public int compare(final Object o1, final Object o2)
      {
         Class left = (Class)o1;
         Class right = (Class)o2;

         int compare = 0;

         if ((compare = compareClassLoader(left.getClassLoader(), right.getClassLoader())) != 0)
         {
            return compare;
         }

         return left.getName().compareTo(right.getName());
      }

      public int compareClassLoader(final ClassLoader left, final ClassLoader right)
      {
         if (left == null ||
               right == null)
         {
            if (left == right)
            {
               return 0;
            }
            else if (left == null)
            {
               return -1;
            }
            else
            {
               return 1;
            }
         }
         else
         {
            return left.toString().compareTo(right.toString());
         }
      }

   }

   static class ClassSorterByClassName implements Comparator
   {

      public int compare(final Object o1, final Object o2)
      {
         Class left = (Class)o1;
         Class right = (Class)o2;
         int compare = left.getName().compareTo(right.getName());
         /*
          * if (compare==0) { if (o1==o2) { return 0; } else { return 1; } }
          * else { return compare; }
          */
         return compare;
      }
   }

   public String convertToString(final Object obj, final boolean callToString)
   {

      String returnValue = null;
      try
      {
         if (obj == null)
         {
            returnValue = "null";
         }
         else
         {
            if (callToString)
            {
               returnValue = "TOSTRING(" +
                     obj.toString() + "), class=" + obj.getClass().getName();
            }
            else
            {
               if (obj instanceof Class)
               {
                  returnValue = "CLASS(" +
                        obj.toString() + "), identifyHashCode=" + System.identityHashCode(obj);
               }
               else
               {
                  returnValue = "OBJ(" +
                        obj.getClass().getName() + "@" + System.identityHashCode(obj) + ")";
               }
            }
         }
      }
      catch (Throwable e)
      {
         return obj.getClass().getName() +
               " toString had an Exception ";
      }

      if (returnValue.length() > 200)
      {
         return "OBJ(" +
               obj.getClass().getName() + "@" + System.identityHashCode(obj) + ")";
      }
      else
      {
         return returnValue;
      }
   }

   /** Explore references recursevely */
   private void exploreObject(final PrintWriter out, final Object source, final int currentLevel, final int maxLevel,
         final boolean useToString, final boolean weakAndSoft, final Map mapDataPoints, final HashSet alreadyExplored)
   {
      String level = null;
      {
         StringBuffer levelStr = new StringBuffer();
         for (int i = 0; i <= currentLevel; i++)
         {
            levelStr.append("!--");
         }
         level = levelStr.toString();
      }

      if (maxLevel >= 0 &&
            currentLevel >= maxLevel)
      {
         out.println("<br>" +
               level + "<b>MaxLevel</b>");
         return;
      }
      Integer index = new Integer(System.identityHashCode(source));

      if (alreadyExplored.contains(index))
      {
         if (source instanceof Class)
         {
            out.println("<br>" +
                  level + " object instanceOf " + source + "@" + index + " was already described before on this report");
         }
         else
         {
            out.println("<br>" +
                  level + " object instanceOf " + source.getClass() + "@" + index +
                  " was already described before on this report");
         }
         return;
      }

      alreadyExplored.add(index);

      Long sourceTag = new Long(getTagOnObject(source));
      ArrayList listPoints = (ArrayList)mapDataPoints.get(sourceTag);
      if (listPoints == null)
      {
         return;
      }

      Iterator iter = listPoints.iterator();

      while (iter.hasNext())
      {
         ReferenceDataPoint point = (ReferenceDataPoint)iter.next();

         Object nextReference = treatReference(level, out, point, useToString);

         if (nextReference != null &&
               !weakAndSoft)
         {
            if (nextReference instanceof WeakReference ||
                  nextReference instanceof SoftReference)
            {
               nextReference = null;
            }
         }

         if (nextReference != null)
         {
            exploreObject(out,
                          nextReference,
                          currentLevel + 1,
                          maxLevel,
                          useToString,
                          weakAndSoft,
                          mapDataPoints,
                          alreadyExplored);
         }
      }

   }

   /**
    * This is used by JSPs to have access to internal features formating
    * results according to the navigations. That's the only reason this method
    * is public. This is not intended to be used as part of the public API.
    * 
    * @urlBaseToFollow will be concatenated objId=3> obj </a> to the
    *                  outputStream
    */
   public Object treatReference(final String level, final PrintWriter out, final ReferenceDataPoint point,
         final boolean useToString)
   {
      Object referenceHolder = null;
      if (point.getReferenceHolder() == 0 ||
            point.getReferenceHolder() == -1)
      {
         referenceHolder = null;
      }
      else
      {
         referenceHolder = getObjectOnTag(point.getReferenceHolder());
      }
      Object nextReference = null;
      switch (point.getReferenceType())
      {
         case JVMTITypes.JVMTI_REFERENCE_INSTANCE:
            ;// Reference from an object to its class.
            out.println("<br>" +
                  level + "InstanceOfReference:ToString=" + convertToString(referenceHolder, useToString));

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

            if (referenceHolder == null)
            {
               fieldName = "Reference GONE";
            }
            else
            {
               Class clazz = referenceHolder.getClass();
               Field field = getObjectField(clazz, (int)point.getIndex());
               if (field == null)
               {
                  fieldName = "UndefinedField@" +
                        referenceHolder;
               }
               else
               {
                  fieldName = field.toString();
               }
            }
            out.println("<br>" +
                  level + " FieldReference " + fieldName + "=" + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         }
         case JVMTITypes.JVMTI_REFERENCE_ARRAY_ELEMENT:// Reference from an array
            // to one of its
            // elements. For
            // references of this kind the referrer_index parameter to the
            // jvmtiObjectReferenceCallback is the array index.
            if (referenceHolder == null)
            {
               out.println("<br>" +
                     level + " arrayRef Position " + point.getIndex() + " is gone");
            }
            else
            {
               out.println("<br>" +
                     level + " arrayRef " + referenceHolder.getClass().getName() + "[" + point.getIndex() + "] id=@" +
                     System.identityHashCode(referenceHolder));
            }
            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_CLASS_LOADER:// Reference from a class
            // to its class loader.
            out.println("<br>" +
                  level + "ClassLoaderReference @ " + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_SIGNERS:// Reference from a class to its
            // signers array.
            out.println("<br>" +
                  level + "ReferenceSigner@" + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_PROTECTION_DOMAIN:// Reference from a
            // class to its
            // protection
            // domain.
            out.println("<br>" +
                  level + "ProtectionDomain@" + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTITypes.JVMTI_REFERENCE_INTERFACE:// Reference from a class to
            // one of its interfaces.
            out.println("<br>" +
                  level + "ReferenceInterface@" + convertToString(referenceHolder, useToString));
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
            Class clazz = (Class)referenceHolder;
            Field field = getObjectField(clazz, (int)point.getIndex());
            String fieldName = null;
            if (field == null)
            {
               fieldName = "UndefinedField@" +
                     referenceHolder;
            }
            else
            {
               fieldName = field.toString();
            }
            out.println("<br>" +
                  level + " StaticFieldReference " + fieldName);
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
            out.println("<br>" +
                  level + "ReferenceInterface@" + convertToString(referenceHolder, useToString));
            nextReference = referenceHolder;
            break;
         case JVMTITypes.ROOT_REFERENCE:
            out.println("<br>" +
                  level + "Root");
            nextReference = null;
            break;
         case JVMTITypes.THREAD_REFERENCE:

            Class methodClass = getMethodClass(point.getMethod());
            if (methodClass != null)
            {
               String className = null;
               if (methodClass != null)
               {
                  className = methodClass.getName();
               }

               String methodName = getMethodName(point.getMethod());
               out.println("<br>" +
                     level + " Reference inside a method - " + className + "::" + methodName);
            }
            nextReference = null;
            break;
         default:
            System.out.println("unexpected reference " +
                  point);
      }
      return nextReference;
   }

   /**
    * This method tags the JVM and return an index. You can navigate through
    * references using this returned HashMap. This method can't be exposed
    * through JMX as it would serialize a huge amount of data.
    * 
    * @return HashMap<Long objectId,ArrayList<ReferenceDataPoint> referencees>
    * */
   public Map<Long, List<ReferenceDataPoint>> createIndexMatrix() throws IOException
   {
      releaseTags();
      final HashMap<Long, List<ReferenceDataPoint>> referencesMap = new HashMap<Long, List<ReferenceDataPoint>>();
      File tmpFile = File.createTempFile("tmpRefs", ".tmp");
      notifyOnReferences(tmpFile.getAbsolutePath(), new JVMTICallBack()
      {
         int count = 0;

         public void notifyReference(final long referenceHolder, final long referencedObject, final long classTag,
               final long index, final long method, final byte referenceType)
         {
            ReferenceDataPoint dataPoint =
                  new ReferenceDataPoint(referenceHolder, referencedObject, classTag, index, method, referenceType);
            Long indexLong = new Long(referencedObject);
            List<ReferenceDataPoint> arrayList = (ArrayList<ReferenceDataPoint>)referencesMap.get(indexLong);
            if (arrayList == null)
            {
               arrayList = new ArrayList<ReferenceDataPoint>();
               referencesMap.put(indexLong, arrayList);
            }
            arrayList.add(dataPoint);
         }

         public void notifyClass(final long classTag, final Class clazz)
         {
         }

         public void notifyObject(final long classTag, final long objectId, final long bytes)
         {
         }
      });

      tmpFile.delete();

      return referencesMap;
   }

   /**
    * Show the reference holders tree of an object
    * 
    * @param className
    *            The name of the class to explore
    * @param maxLevel
    *            The number of levels to explode. Be careful as if you put this
    *            number too high, you migh endup in a forever loop, specially
    *            if your object is referencing something too generic
    * @param solveReferencesOnClass
    *            Will expose the tree on the class
    * @param solveReferencesOnClassLoader
    *            Will expode the tree on the classLoader (I mostly recommend to
    *            only look for classLoader's references)
    * @param useToString
    *            If true, will use toString when an object is printed. If False
    *            will use className@<System.identityHashCode(object)>
    * @param weakAndSoft
    *            If false, won't detail references on Weak and Soft References
    * @param printObject
    *            If true, Will print (with toString) every single instance of
    *            the object passed as parameter
    */
   public String exploreClassReferences(final String className, final int maxLevel,
         final boolean solveReferencesOnClasses, final boolean solveReferencesOnClassLoaders,
         final boolean useToString, final boolean weakAndSoft, final boolean printObjects)
   {
      forceGC();
      if (!solveReferencesOnClasses &&
            !solveReferencesOnClassLoaders && !printObjects)
      {
         return "<b> you have to select at least solveReferences || solveClassLoaders || printObjects </b>";
      }

      Map referencesMap = null;
      try
      {
         referencesMap = createIndexMatrix();
      }
      catch (Exception e)
      {
         CharArrayWriter charArray = new CharArrayWriter();
         PrintWriter out = new PrintWriter(charArray);
         e.printStackTrace(out);
         return charArray.toString();
      }

      try
      {
         CharArrayWriter charArray = new CharArrayWriter();
         PrintWriter out = new PrintWriter(charArray);

         out.println(exploreClassReferences(className,
                                            maxLevel,
                                            solveReferencesOnClasses,
                                            solveReferencesOnClassLoaders,
                                            useToString,
                                            weakAndSoft,
                                            referencesMap));

         if (printObjects)
         {
            releaseTags();
            Class classes[] = getLoadedClasses();

            for (Class clazz : classes)
            {
               if (clazz.getName().equals(className))
               {
                  Object[] objs = this.getAllObjects(clazz);

                  if (objs.length != 0)
                  {
                     out.println("<br> Instances of:" +
                           clazz.getName() + " ClassLoader=" + clazz.getClassLoader());
                     for (int countOBJ = 0; countOBJ < objs.length; countOBJ++)
                     {
                        out.println("<br>" +
                              clazz.getName() + "[" + countOBJ + "]=" + objs[countOBJ]);
                     }
                  }
               }
            }
         }

         out.flush();

         return charArray.toString();
      }
      finally
      {
         referencesMap.clear();
         releaseTags();
      }
   }

   /**
    * This is an overload to reuse the matrix index in case you already have
    * indexed the JVM
    */
   public String exploreClassReferences(final String className, final int maxLevel,
         final boolean solveReferencesOnClasses, final boolean solveReferencesOnClassLoaders,
         final boolean useToString, final boolean weakAndSoft, final Map referencesMap)
   {
      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      try
      {

         Class[] loadClasses = getLoadedClasses();

         for (Class loadClasse : loadClasses)
         {
            if (loadClasse.getName().equals(className))
            {
               out.println("<br><br><br><b>References to " +
                     loadClasse + "</b>");
               if (solveReferencesOnClasses)
               {
                  exploreObject(out, loadClasse, 0, maxLevel, useToString, weakAndSoft, referencesMap, new HashSet());
               }
               if (solveReferencesOnClassLoaders)
               {
                  if (loadClasse.getClassLoader() != null)
                  {
                     out.println("<br><b><i>references to its classloader " +
                           loadClasse.getClassLoader() + "</i></b>");
                     exploreObject(out,
                                   loadClasse.getClassLoader(),
                                   0,
                                   maxLevel,
                                   useToString,
                                   weakAndSoft,
                                   referencesMap,
                                   new HashSet());
                  }
               }
               out.println("<br>");

            }
         }

         loadClasses = null;

         return charArray.toString();
      }
      catch (Exception e)
      {
         charArray = new CharArrayWriter();
         out = new PrintWriter(charArray);
         e.printStackTrace(out);
         return charArray.toString();
      }
   }

   /**
    * Show the reference holders tree of an object. This method is also exposed
    * through MBean.
    */
   public String exploreObjectReferences(final String className, final int maxLevel, final boolean useToString)
   {
      forceGC();

      Object obj[] = this.getAllObjects(className);

      System.out.println("Obj.length = " +
            obj.length);

      Map referencesMap = null;
      try
      {
         referencesMap = createIndexMatrix();
      }
      catch (Exception e)
      {
         CharArrayWriter charArray = new CharArrayWriter();
         PrintWriter out = new PrintWriter(charArray);
         e.printStackTrace(out);
         return charArray.toString();
      }

      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      try
      {
         for (int i = 0; i < Math.min(50, obj.length); i++)
         {
            out.println("<br><b>References to obj[" +
                  i + "]=" + (useToString
                        ? obj[i].toString() : obj[i].getClass().getName()));
            out.println(exploreObjectReferences(referencesMap, obj[i], maxLevel, useToString));
         }
         return charArray.toString();
      }
      finally
      {
         referencesMap.clear();
         releaseTags();
      }
   }

   /**
    * Show the reference holders tree of an object. This returns a report you
    * can visualize through MBean.
    */
   public String exploreObjectReferences(final Map referencesMap, final Object thatObject, final int maxLevel,
         final boolean useToString)
   {
      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      try
      {
         exploreObject(out, thatObject, 0, maxLevel, useToString, false, referencesMap, new HashSet());
         out.println("<br>");
         return charArray.toString();
      }
      catch (Exception e)
      {
         charArray = new CharArrayWriter();
         out = new PrintWriter(charArray);
         e.printStackTrace(out);
         return charArray.toString();
      }
   }

   /**
    * Forces an OutOfMemoryError and releases the memory immediatly. This will
    * force SoftReferences to go away.
    */
   public void forceReleaseOnSoftReferences()
   {
      SoftReference reference = new SoftReference(new Object());

      ArrayList list = new ArrayList();
      int i = 0;
      try
      {
         while (true)
         {
            list.add("A Big String A Big String A Big String A Big String A Big String A Big String A Big String A Big String A Big String A Big String A Big String " +
                  i++);
            if (i % 1000 == 0) // doing the check on each 100 elements
            {
               if (reference.get() == null)
               {
                  System.out.println("Break as the soft reference was gone");
                  break;
               }
            }
         }
      }
      catch (Throwable e)
      {
      }

      list.clear();
      try
      {
         ByteArrayOutputStream byteout = new ByteArrayOutputStream();
         ObjectOutputStream out = new ObjectOutputStream(byteout);

         out.writeObject(new Dummy());

         ByteArrayInputStream byteInput = new ByteArrayInputStream(byteout.toByteArray());
         ObjectInputStream input = new ObjectInputStream(byteInput);
         input.readObject();

      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      forceGC();
   }

   /**
    * Used just to serialize anything and release SoftCache on java
    * Serialization
    */
   static class Dummy implements Serializable
   {
      private static final long serialVersionUID = 1L;
   }

   /**
    * Will show a report of every class loaded on this JVM. At the beggining of
    * the report you will see duplicated classes (classes loaded in more than
    * one classLoader)
    */
   public String listClassesHTMLReport() throws Exception
   {
      try
      {
         forceGC();
         CharArrayWriter charArray = new CharArrayWriter();
         PrintWriter out = new PrintWriter(charArray);

         Collection classSet = createTreeSet(new ClassSorterByClassName());

         boolean printedHeader = false;

         ClassLoader systemClassLoaderDummy = new ClassLoader()
         {
            public String toString()
            {
               return "SystemClassLoader";
            }
         };
         ArrayList classLoaderDuplicates = new ArrayList();
         Iterator iter = classSet.iterator();
         String currentName = null;
         Class currentClass = null;
         while (iter.hasNext())
         {
            currentClass = (Class)iter.next();
            if (currentName != currentClass.getName())
            {
               if (classLoaderDuplicates.size() > 1)
               {
                  if (!printedHeader)
                  {
                     out.println("<br><b>List of duplicated classes</b>");
                     printedHeader = true;
                  }

                  out.println("<br>" +
                        "<b> Class " + currentName + " was loaded on these classLoaders:</b>");
                  Iterator iterClassLoader = classLoaderDuplicates.iterator();
                  while (iterClassLoader.hasNext())
                  {
                     ClassLoader loader = (ClassLoader)iterClassLoader.next();
                     out.println("<br>" +
                           loader.toString());
                  }

               }
               currentName = currentClass.getName();
               classLoaderDuplicates.clear();
            }

            ClassLoader loader = currentClass.getClassLoader();
            if (loader == null)
            {
               loader = systemClassLoaderDummy;
            }
            classLoaderDuplicates.add(loader);

            currentName = currentClass.getName();
         }

         if (classLoaderDuplicates.size() > 1)
         {
            out.println("<br>" +
                  "<b> Class " + currentName + " was loaded on these classLoaders:</b>");
            Iterator iterClassLoader = classLoaderDuplicates.iterator();
            while (iterClassLoader.hasNext())
            {
               ClassLoader loader = (ClassLoader)iterClassLoader.next();
               out.println("<br>" +
                     loader.toString());
            }

         }

         out.println("<br><b>List of classes by ClassLoader</b>");
         classSet = retrieveLoadedClassesByClassLoader();

         // I will need a dummy reference, as the first classLoader on the
         // iterator will be null
         ClassLoader currentClassLoader = new ClassLoader()
         {
         };
         out.println("<br>");

         iter = classSet.iterator();
         while (iter.hasNext())
         {
            Class clazz = (Class)iter.next();
            if (currentClassLoader != clazz.getClassLoader())
            {
               currentClassLoader = clazz.getClassLoader();
               out.println("<br><b>ClassLoader = " +
                     (currentClassLoader == null
                           ? "System Class Loader" : currentClassLoader.toString()) + "</b>");
            }
            out.println("Class = " +
                  clazz.getName());
         }

         return new String(charArray.toCharArray());
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw e;
      }
   }

   /** Used by JSPs and JMX to report */
   public Collection retrieveLoadedClassesByClassName()
   {
      Collection classSet;
      classSet = createTreeSet(new ClassSorterByClassName());
      return classSet;
   }

   /** Used by JSPs and JMX to report */
   public Collection retrieveLoadedClassesByClassLoader()
   {
      Collection classSet;
      classSet = createTreeSet(new ClassSorterByClassLoader());
      return classSet;
   }

   private Collection createTreeSet(final Comparator comparator)
   {
      Class[] classes = getLoadedClasses();

      ArrayList classSet = new ArrayList();

      for (Class classe : classes)
      {
         classSet.add(classe);
      }

      Collections.sort(classSet, comparator);
      return classSet;
   }

   static class InnerCallBack implements JVMTICallBack
   {

      HashMap<Long, Class<?>> classesMap = new HashMap<Long, Class<?>>();

      WeakHashMap<Class<?>, InventoryDataPoint> maps = new WeakHashMap<Class<?>, InventoryDataPoint>();

      public void notifyClass(final long classTag, final Class<?> clazz)
      {
         classesMap.put(classTag, clazz);
      }

      public void notifyObject(final long classTag, final long objectId, final long bytes)
      {
         Class<?> clazz = (Class<?>)classesMap.get(classTag);

         if (clazz != null) // this is not supposed to happen, but just in
         // case I keep this if here
         {
            InventoryDataPoint point = (InventoryDataPoint)maps.get(clazz);
            if (point == null)
            {
               point = new InventoryDataPoint(clazz);
               maps.put(clazz, point);
            }

            point.bytes += bytes;
            point.instances++;
         }

      }

      public void notifyReference(final long referenceHolder, final long referencedObject, final long classTag,
            final long index, final long method, final byte referenceType)
      {
      }

   }

   /**
    * It will return true if the comparisson didn't represent any changes. This
    * can be used by JUnitTests to validate the consumption of the memory is on
    * the expected results.
    * 
    * @param reportOutput
    *            You could set System.out here. The location where logging
    *            information is going to be sent.
    * @param map1
    *            The first snapshot.
    * @param map2
    *            The second snapshot.
    * @param ignoredClasses
    *            Classes you want to ignore on the comparisson. Used to ignore
    *            things you know are going to be produced and you don't have
    *            control over the testcase.
    * @param prefixesToIgnore
    *            Same thing as classes, but every classes starting with these
    *            prefixes are going to be ignored.
    * @param expectedIncreases
    *            An array of InventoryDataPoint with the maximum number of
    *            instances each class could be generating.
    * @return true if the assertion is okay
    */
   public boolean compareInventories(final PrintStream reportOutput, final Map map1, final Map map2,
         final Class[] ignoredClasses, final String[] prefixesToIgnore, final InventoryDataPoint[] expectedIncreases)
   {
      HashSet ignoredItems = new HashSet();
      if (ignoredClasses != null)
      {
         for (Class ignoredClasse : ignoredClasses)
         {
            ignoredItems.add(ignoredClasse);
         }
      }

      HashMap expectedIncreasesHash = new HashMap();
      if (expectedIncreases != null)
      {
         for (InventoryDataPoint expectedIncrease : expectedIncreases)
         {
            Class clazz = expectedIncrease.getClazz();
            expectedIncreasesHash.put(clazz, expectedIncrease);
         }
      }

      // expected increase based on map1's size
      addExpectedIncrease(expectedIncreasesHash, "java.lang.ref.ReferenceQueue$Lock", 1);
      addExpectedIncrease(expectedIncreasesHash, "java.util.WeakHashMap", 1);
      addExpectedIncrease(expectedIncreasesHash, "java.lang.ref.ReferenceQueue", 1);
      addExpectedIncrease(expectedIncreasesHash, "[Ljava.util.WeakHashMap$Entry;", 1);
      addExpectedIncrease(expectedIncreasesHash, "java.lang.ref.WeakReference", map1.size());
      addExpectedIncrease(expectedIncreasesHash, "java.util.WeakHashMap$Entry", map1.size());

      boolean reportOK = true;

      Iterator iterMap1 = map1.entrySet().iterator();
      while (iterMap1.hasNext())
      {
         Map.Entry entry = (Map.Entry)iterMap1.next();
         Class clazz = (Class)entry.getKey();

         boolean isIgnoredPrefix = false;
         if (prefixesToIgnore != null)
         {
            for (String element : prefixesToIgnore)
            {
               if (clazz.getName().startsWith(element))
               {
                  isIgnoredPrefix = true;
                  break;
               }
            }
         }
         if (!isIgnoredPrefix &&
               !ignoredItems.contains(entry.getKey()))
         {
            InventoryDataPoint point1 = (InventoryDataPoint)entry.getValue();
            InventoryDataPoint point2 = (InventoryDataPoint)map2.get(clazz);
            if (point2 != null)
            {
               if (point2.getInstances() > point1.getInstances())
               {
                  InventoryDataPoint expectedIncrease = (InventoryDataPoint)expectedIncreasesHash.get(clazz);
                  boolean failed = true;
                  if (expectedIncrease != null)
                  {
                     if (point2.getInstances() -
                           point1.getInstances() <= expectedIncrease.getInstances())
                     {
                        failed = false;
                     }
                  }
                  if (failed)
                  {
                     int expected = 0;
                     if (expectedIncrease != null)
                     {
                        expected = expectedIncrease.getInstances();
                     }
                     reportOK = false;
                     reportOutput.println("<br> Class " +
                           clazz.getName() + " had an increase of " + (point2.getInstances() -
                                 point1.getInstances() - expected) + " instances represented by " +
                           (point2.getBytes() - point1.getBytes()) + " bytes");
                     if (expectedIncrease != null)
                     {
                        reportOutput.print("<br> " +
                              (point2.getInstances() -
                                    point1.getInstances() - expectedIncrease.getInstances()) + " higher than expected");
                     }

                  }
               }
            }
         }
      }

      return reportOK;
   }

   private void addExpectedIncrease(final HashMap expectedIncreasesHash, final String name, final int numberOfInstances)
   {
      Class tmpClass = getClassByName(name);
      if (tmpClass != null)
      {
         expectedIncreasesHash.put(tmpClass, new InventoryDataPoint(tmpClass, numberOfInstances));
      }
   }

   /**
    * Returns a WeakHashMap<Class,InventoryDataPoint> summarizing the current
    * JVM's inventory.
    * */
   public synchronized Map<Class<?>, InventoryDataPoint> produceInventory() throws IOException
   {
      forceGC();
      InnerCallBack callBack = new InnerCallBack();
      File tmpFileObjects = File.createTempFile("delete-me", ".objects");
      try
      {
         notifyInventory(true, null, tmpFileObjects.getAbsolutePath(), callBack);
      }
      finally
      {
         if (tmpFileObjects.exists())
         {
            try
            {
               tmpFileObjects.delete();
            }
            catch (Exception ignored)
            {
            }
         }
      }

      return callBack.maps;
   }

   public String inventoryReport() throws Exception
   {
      return inventoryReport(true);
   }

   /** Will list the current memory inventory. Exposed through JMX. */
   public synchronized String inventoryReport(final boolean html) throws Exception
   {
      Map map = produceInventory();

      TreeSet valuesSet = new TreeSet(map.values());
      Iterator iterDataPoints = valuesSet.iterator();
      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      if (html)
      {
         out.println("<table><tr><td>Class</td><td>#Instances</td><td>#Bytes</td></tr>");
      }
      else
      {
         out.println(String.format("|%1$-100s|%2$10s|%3$10s|", "Class", "Instances", "Bytes"));
      }

      while (iterDataPoints.hasNext())
      {
         InventoryDataPoint point = (InventoryDataPoint)iterDataPoints.next();
         if (html)
         {
            out.println("<tr><td>" +
                  point.getClazz().getName() + "</td><td>" + point.getInstances() + "</td><td>" + point.getBytes() +
                  "</td></tr>");
         }
         else
         {
            out.println(String.format("|%1$-100s|%2$10d|%3$10d|",
                                      point.getClazz().getName(),
                                      point.getInstances(),
                                      point.getBytes()));
         }
      }

      if (html)
      {
         out.println("</table>");
      }

      return charArray.toString();
   }

   /**
    * Will print a report of every instance of the class passed by parameter.
    * Exposed through JMX .
    */
   public String printObjects(final String className) throws Exception
   {
      CharArrayWriter charArray = new CharArrayWriter();
      PrintWriter out = new PrintWriter(charArray);

      Object objects[] = this.getAllObjects(className);

      out.println("<table>");
      for (Object object : objects)
      {
         out.println("<tr><td>");
         out.println(object);
         if (object instanceof Object[])
         {
            out.println("</td><td>");
            out.println("array of " +
                  ((Object[])object).length);
            out.println("</td></tr>");
         }
      }

      out.println("</table>");

      return charArray.toString();
   }

}
