/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.j2ee.client;

import java.util.jar.JarFile;

import com.ibm.wala.classLoader.Module;

/**
 * 
 * An AnalysisEngine analyzes one or more J2EE modules, including
 * ear files, J2EE clients, web modules, and ejb modules.
 * 
 * @author Logan Colby
 * @author Stephen Fink
 */

public interface J2EEAnalysisEngine extends com.ibm.wala.client.AnalysisEngine {

  /**
   * Specify the jar file that represent the contents of the j2ee.jar 
   * that the application relies on
   * 
   * @param libs an array of jar files; for WAS, j2ee.jar and webcontainer.jar
   */
  void setJ2EELibraries(JarFile[] libs);
  
  /**
   * Specify the mdoules that represent the contents of the j2ee.jar 
   * that the application relies on
   * 
   * @param libs an array of Modules; for WAS, j2ee.jar and webcontainer.jar
   */
  void setJ2EELibraries(Module[] libs);

}
