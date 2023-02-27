/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.checkleak.maven;

import java.io.File;
import java.util.Properties;

import io.github.checkleak.core.Installer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * This will resolve the native agent library
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class InstallLib extends AbstractPlugin {

   @Parameter
   String target;

   @Parameter
   String lib;


   @Parameter
   String[] args;

   @Parameter(defaultValue = "${noClient}")
   boolean ignore;

   /**
    * @parameter
    */
   private Properties systemProperties;

   @Override
   protected boolean isIgnore() {
      return ignore;
   }

   @Override
   protected void doExecute() throws MojoExecutionException, MojoFailureException {
      if (target == null) {
         throw new RuntimeException("target not defined");
      }

      File targetFile = new File(target);
      targetFile.mkdirs();

      File targetLib = new File(targetFile, lib);
      try {
         Installer.install(targetLib);
      } catch (Exception e) {
         getLog().warn(e.getMessage(), e);
      }

   }
}
