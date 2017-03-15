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
package com.ibm.wala.client;

import java.util.Collection;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.Entrypoint;

public interface AnalysisEngine {

  /**
   * Specify the list of modules that should be analyzed. If an EARFile is included in the list, all of its contained modules should
   * be examined. Multiple ear files can be specified for cross-app invocations, which will become increasingly common in the 5.1
   * release.
   * 
   * @param moduleFiles A non-null Collection of module files: (EARFile, WARFile, ApplicationClientFile, EJBJarFile).
   */
  void setModuleFiles(Collection<? extends Module> moduleFiles);

  /**
   * Specify the jar files that represent the standard J2SE libraries
   * 
   * @param libs an array of jar files; usually rt.jar for vanilla JDK core.jar, server.jar, and xml.jar for some WAS runtimes
   */
  void setJ2SELibraries(JarFile[] libs);

  /**
   * Specify the mdoules that represent the standard J2SE libraries
   * 
   * @param libs an array of Modules; usually rt.jar for vanilla JDK core.jar, server.jar, and xml.jar for some WAS runtimes
   */
  void setJ2SELibraries(Module[] libs);

  /**
   * Specify whether the engine should or should not employ "closed-world" analysis.
   * 
   * In a closed-world analysis, the engine considers only application client main methods and servlet entrypoints to the
   * application.
   * 
   * In an open-world analysis, the engine additionally considers all EJB local and remote interface methods as entrypoints.
   * 
   * By default, this property is false; the default analysis is open-world
   * 
   * @param b whether to use closed-world analysis
   */
  void setClosedWorld(boolean b);

  /**
   * Get the default analysis options appropriate for this engine
   */
  AnalysisOptions getDefaultOptions(Iterable<Entrypoint> entrypoints);
}
