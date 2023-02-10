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
 * Interface to get notifications for references on JVMTI
 * @author Clebert Suconic
 *
 */
public interface JVMTICallBack
{
   /**
    *JNISignature for this method = (JJJJJB)V
    * @param referenceHolder A tag id of an object  (if -1 means the root)
    * @param referencedObject A tag id of an object
    * @param fieldId A tag id for a Field referencing the object. If -1 that means a static reference in a method
    * @param classTag A tag for the class holding the reference
    * @param index The index about the operation (like the field ID, array number.. etc)
    * @param method The method id (look at JVMTI docs)
    * @param referenceType Look at {@link JVMTITypes}
    */
   public void notifyReference(long referenceHolder,
                               long referencedObject,
                               long classTag,
                               long index,
                               long method,
                               byte referenceType);

   /**
    * Notification about a class
    * @param classTag the class tag id
    * @param clazz the class name
    */
   public void notifyClass(long classTag, Class<?> clazz);

   /** 
    * Notification about an object
    * @param classTag The class tag id
    * @param objectId the object id
    * @param bytes the number of bytes
    */
   public void notifyObject(long classTag, long objectId, long bytes);

}
