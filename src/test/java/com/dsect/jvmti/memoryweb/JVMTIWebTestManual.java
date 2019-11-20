/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package com.dsect.jvmti.memoryweb;

import com.dsect.jvmti.JVMTIInterface;
import com.dsect.jvmti.TestClass;
import org.junit.Test;


public class JVMTIWebTestManual {

   TestClass str2[] = null;

   TestClass strroot;

   static TestClass staticField;

   public static void main(String arg[]) {
      try {
         JVMTIWebTestManual manual = new JVMTIWebTestManual();
         manual.testWeb();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


   @Test
   public void testWeb() throws Exception {
      TestClass str[] = new TestClass[1000];

      for (int i = 0; i < 1000; i++) {
         str[i] = new TestClass();
      }

      str2 = str;

      TestClass strroot2 = str[0];
      strroot = str[0];

      staticField = str[0];

      JVMTIInterface jvmti = new JVMTIInterface();

      System.out.println(jvmti.inventoryReport(false));

      System.out.println("strroot = " + strroot2);
   }

}
