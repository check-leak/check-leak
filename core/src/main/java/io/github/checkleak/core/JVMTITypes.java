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

/**
 * A JVMTITypes
 *
 * @author <mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 *
 *
 */
public class JVMTITypes
{

   public static String toString(final int referenceType)
   {
      switch (referenceType)
      {
         case JVMTI_REFERENCE_INSTANCE:
            return "JVMTI_REFERENCE_CLASS";

         case JVMTI_REFERENCE_FIELD:
            return "JVMTI_REFERENCE_FIELD";
         case JVMTI_REFERENCE_ARRAY_ELEMENT:
            return "JVMTI_REFERENCE_ARRAY_ELEMENT";

         case JVMTI_REFERENCE_CLASS_LOADER:
            return "JVMTI_REFERENCE_CLASS_LOADER";

         case JVMTI_REFERENCE_SIGNERS:
            return "JVMTI_REFERENCE_SIGNERS";

         case JVMTI_REFERENCE_PROTECTION_DOMAIN:
            return "JVMTI_REFERENCE_PROTECTION_DOMAIN";

         case JVMTI_REFERENCE_INTERFACE:
            return "JVMTI_REFERENCE_INTERFACE";

         case JVMTI_REFERENCE_STATIC_FIELD:
            return "JVMTI_REFERENCE_STATIC_FIELD";

         case JVMTI_REFERENCE_CONSTANT_POOL:
            return "JVMTI_REFERENCE_CONSTANT_POOL";

         case ROOT_REFERENCE:
            return "ROOT_REFERENCE";

         case THREAD_REFERENCE:
            return "THREAD_REFERENCE";

         default:
            return "UnknownReferenceType(" +
                  referenceType + ")";

      }
   }

   // Constants -----------------------------------------------------
   public static final int JVMTI_REFERENCE_INSTANCE = 1;// Reference from an object to its class.

   public static final int JVMTI_REFERENCE_FIELD = 2;// Reference from an object to the value of one of its instance

   // fields. For references of this kind the referrer_index parameter
   // to the jvmtiObjectReferenceCallback is the index of the the
   // instance field. The index is based on the order of all the
   // object's fields. This includes all fields of the directly
   // declared static and instance fields in the class, and includes
   // all fields (both public and private) fields declared in
   // superclasses and superinterfaces. The index is thus calculated
   // by summing the index of field in the directly declared class
   // (see GetClassFields), with the total number of fields (both
   // public and private) declared in all superclasses and
   // superinterfaces. The index starts at zero.

   public static final int JVMTI_REFERENCE_ARRAY_ELEMENT = 3;// Reference from an array to one of its elements. For

   // references of this kind the referrer_index parameter to
   // the jvmtiObjectReferenceCallback is the array index.

   public static final int JVMTI_REFERENCE_CLASS_LOADER = 4;// Reference from a class to its class loader.

   public static final int JVMTI_REFERENCE_SIGNERS = 5;// Reference from a class to its signers array.

   public static final int JVMTI_REFERENCE_PROTECTION_DOMAIN = 6;// Reference from a class to its protection domain.

   public static final int JVMTI_REFERENCE_INTERFACE = 7;// Reference from a class to one of its interfaces.

   public static final int JVMTI_REFERENCE_STATIC_FIELD = 8;// Reference from a class to the value of one of its static

   // fields. For references of this kind the referrer_index
   // parameter to the jvmtiObjectReferenceCallback is the
   // index of the static field. The index is based on the
   // order of the directly declared static and instance fields
   // in the class (not inherited fields), starting at zero.
   // See GetClassFields.

   public static final int JVMTI_REFERENCE_CONSTANT_POOL = 9;// Reference from a class to a resolved entry in the

   // constant pool. For references of this kind the
   // referrer_index parameter to the
   // jvmtiObjectReferenceCallback is the index into constant
   // pool table of the class, starting at 1. See The Constant
   // Pool in the Java Virtual Machine Specification.

   public static final int ROOT_REFERENCE = 10;

   public static final int THREAD_REFERENCE = 11;

}
