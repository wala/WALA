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
package com.ibm.wala.j2ee.client.impl;

import java.util.Iterator;
import java.util.jar.JarFile;

import org.eclipse.jst.j2ee.commonarchivecore.internal.Archive;

import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.client.impl.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.j2ee.DeploymentMetaData;
import com.ibm.wala.j2ee.J2EEAnalysisScope;
import com.ibm.wala.j2ee.client.J2EECallGraphBuilderFactory;
import com.ibm.wala.j2ee.client.J2EEAnalysisEngine;
import com.ibm.wala.j2ee.util.TopLevelArchiveModule;
import com.ibm.wala.j2ee.util.TopLevelArchiveModule.BloatedArchiveModule;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * abstract base class for analysis engine implementations
 * 
 * @author sfink
 */
public abstract class J2EEAbstractAnalysisEngine extends AbstractAnalysisEngine implements J2EEAnalysisEngine {

  /**
   * A representation of the deployment descriptor
   */
  private DeploymentMetaData dmd;

  /**
   * The J2EE libraries to analyze
   */
  private Module[] j2eeLibs;

  /**
   * Should we analyze dependent jar files?
   */
  private boolean dependentJars = true;

  protected J2EEAbstractAnalysisEngine() {
  }

  protected CallGraphBuilder getCallGraphBuilder(ClassHierarchy cha, AnalysisOptions options) {
    return ((J2EECallGraphBuilderFactory) getCallGraphBuilderFactory()).make(options, getCache(), cha, (J2EEAnalysisScope) getScope(), getDmd(),
        false);
  }

  /**
   * Set up the AnalysisScope object
   */
  protected void buildAnalysisScope() {
    buildAnalysisScope(null);
  }

  /**
   * Set up the AnalysisScope object
   */
  protected void buildAnalysisScope(String exclusionsFile) {
    // set up the scope of the analysis
    ClassLoader cl = J2EEAbstractAnalysisEngine.class.getClassLoader();
    if (j2seLibs == null) {
      Assertions.UNREACHABLE("no j2selibs specificed. You probably did not call AppAnalysisEngine.setJ2SELibrary.");
    } else if (j2eeLibs == null) {
      Assertions.UNREACHABLE("j2ee.jar is null. You probably did not call AnalysisEngine.setJ2EELibrary.");
    } else {
      scope = J2EEAnalysisScope.make(j2seLibs, j2eeLibs, exclusionsFile, cl, true);
    }

    addApplicationModulesToScope();
  }

  /**
   * Add the application modules to the analysis scope.
   */
  @SuppressWarnings( { "restriction", "unchecked" })
  protected void addApplicationModulesToScope() {
    ClassLoaderReference app = scope.getApplicationLoader();
    for (Iterator<Archive> it = moduleFiles.iterator(); it.hasNext();) {
      Archive A = (Archive) it.next();
      // TODO: redesign to avoid holding onto BloatedArchives?
      TopLevelArchiveModule M = new BloatedArchiveModule(A);
      if (!dependentJars) {
        M.setIgnoreDependentJars(true);
      }
      scope.addToScope(app, M);
    }
  }

  /*
   * @see com.ibm.wala.atk.AppAnalysisEngine#setJ2EELibrary(java.util.jar.JarFile)
   */
  public void setJ2EELibraries(JarFile[] libs) {
    if (libs == null) {
      Assertions.UNREACHABLE("Illegal to setJ2EELibrary(null)");
    }
    this.j2eeLibs = new Module[libs.length];
    for (int i = 0; i < libs.length; i++) {
      j2eeLibs[i] = new JarFileModule(libs[i]);
    }
  }

  public void setJ2EELibraries(Module[] libs) {
    if (libs == null) {
      Assertions.UNREACHABLE("Illegal to setJ2EELibrary(null)");
    }
    this.j2eeLibs = new Module[libs.length];
    for (int i = 0; i < libs.length; i++) {
      j2eeLibs[i] = libs[i];
    }
  }


  public DeploymentMetaData getDmd() {
    return dmd;
  }


  protected void setDmd(DeploymentMetaData dmd) {
    this.dmd = dmd;
  }


  public boolean isDependentJars() {
    return dependentJars;
  }


  public void setDependentJars(boolean dependentJars) {
    this.dependentJars = dependentJars;
  }

}
