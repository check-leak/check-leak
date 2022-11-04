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

package com.dsect.jvmti;

import java.util.ArrayList;

import com.dsect.jvmti.JVMTIInterface;
import com.dsect.jvmti.util.JVMTIReport;
import org.junit.Test;

public class ReportTest {


   @Test
   public void testInventoryReport() throws Exception {

      ArrayList list = new ArrayList();
      for (int i = 0; i < 100000; i++) {
         list.add(new TestClass(null));
      }


      System.out.println(JVMTIReport.inventoryReport(false));

      for (Object el : list) { // this empty statement is here just to make sure JIT Compiler won't remove the list earlier.
      }

      list.clear();
      list = null;

      System.out.println(JVMTIReport.inventoryReport(false));

   }

}
