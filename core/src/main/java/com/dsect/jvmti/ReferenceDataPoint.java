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

public class ReferenceDataPoint
{
   public ReferenceDataPoint(final long referenceHolder,
                             final long referencedObject,
                             final long classTag,
                             final long index,
                             final long method,
                             final byte referenceType)
   {
      this.referenceHolder = referenceHolder;
      this.referencedObject = referencedObject;
      this.classTag = classTag;
      this.index = index;
      this.method = method;
      this.referenceType = referenceType;
   }

   private long referenceHolder;

   private long referencedObject;

   private long classTag;

   private long index;

   private long method;

   private byte referenceType;

   public long getIndex()
   {
      return index;
   }

   public void setIndex(final long index)
   {
      this.index = index;
   }

   public long getMethod()
   {
      return method;
   }

   public void setMethod(final long method)
   {
      this.method = method;
   }

   public long getReferencedObject()
   {
      return referencedObject;
   }

   public void setReferencedObject(final long referencedObject)
   {
      this.referencedObject = referencedObject;
   }

   public long getReferenceHolder()
   {
      return referenceHolder;
   }

   public void setReferenceHolder(final long referenceHolder)
   {
      this.referenceHolder = referenceHolder;
   }

   public byte getReferenceType()
   {
      return referenceType;
   }

   public void setReferenceType(final byte referenceType)
   {
      this.referenceType = referenceType;
   }

   public long getClassTag()
   {
      return classTag;
   }

   public void setClassTag(final long classTag)
   {
      this.classTag = classTag;
   }

   @Override
   public String toString()
   {
      return super.toString() +
            " {referenceHolder=" + referenceHolder + "\n   referencedObject=" + referencedObject + "\n   classTag=" +
            classTag + "\n   index=" + index + "\n   method=" + method + "\n   referenceType=" + referenceType + "}";
   }

}
