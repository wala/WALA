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
package com.ibm.wala.j2ee;

import java.util.Iterator;
import java.util.jar.JarFile;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;

/**
 * 
 * Description of analysis for EJBs
 * 
 * @author sfink
 */
@SuppressWarnings("unchecked")
public class J2EEAnalysisScope extends EMFScopeWrapper {

  private final static String BASIC_FILE = "SyntheticContainerModel.xml";

  private final static String DEFAULT_FILE = "DefaultWebsphereModules.xml";

  private final static String EXCLUSIONS_FILE = "J2EEClassHierarchyExclusions.xml";

  private final boolean lifecycleEntrypoints;

  static {
    J2EEScopePackageImpl.init();
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  /**
   * @param lifecycleEntrypoints
   *          Should EJB lifecycle entrypoints be considered as call graph
   *          entrypoints?
   */
  public J2EEAnalysisScope(String baseScope, ClassLoader loader, boolean lifecycleEntrypoints) {
    this(baseScope, loader, EXCLUSIONS_FILE, lifecycleEntrypoints);
  }

  /**
   * @param lifecycleEntrypoints
   *          Should EJB lifecycle entrypoints be considered as call graph
   *          entrypoints?
   */
  public J2EEAnalysisScope(String baseScope, ClassLoader loader, String exclusionsFile, boolean lifecycleEntrypoints) {
    super(baseScope, exclusionsFile, loader);
    this.lifecycleEntrypoints = lifecycleEntrypoints;
  }


  /**
   * @param lifecycleEntrypoints
   *          Should EJB lifecycle entrypoints be considered as call graph
   *          entrypoints?
   */
  public static J2EEAnalysisScope makeDefault(ClassLoader loader, boolean lifecycleEntrypoints) {
    return new J2EEAnalysisScope(DEFAULT_FILE, loader, lifecycleEntrypoints);
  }

  public static J2EEAnalysisScope make(JarFile[] J2SELibs, JarFile[] J2EELibs, ClassLoader loader, boolean lifecycleEntrypoints) {
    return make(J2SELibs, J2EELibs, EXCLUSIONS_FILE, loader, lifecycleEntrypoints);
  }



  /**
   * @param lifecycleEntrypoints
   *          Should EJB lifecycle entrypoints be considered as call graph
   *          entrypoints?
   */
  public static J2EEAnalysisScope make(JarFile[] J2SELibs, JarFile[] J2EELibs, String exclusionsFile, ClassLoader loader,
      boolean lifecycleEntrypoints) {
    J2EEAnalysisScope scope;
    scope = new J2EEAnalysisScope(BASIC_FILE, loader, exclusionsFile, lifecycleEntrypoints);
    for (int i = 0; i < J2SELibs.length; i++) {
      JarFileModule lib = new JarFileModule(J2SELibs[i]);
      scope.addToScope(scope.getPrimordialLoader(), lib);
    }
    for (int i = 0; i < J2EELibs.length; i++) {
      JarFileModule lib = new JarFileModule(J2EELibs[i]);
      scope.addToScope(scope.getExtensionLoader(), lib);
    }

    return scope;
  }

  /**
   * @param lifecycleEntrypoints
   *          Should EJB lifecycle entrypoints be considered as call graph
   *          entrypoints?
   */
  public static J2EEAnalysisScope make(Module[] J2SELibs, Module[] J2EELibs, String exclusionsFile, ClassLoader loader,
      boolean lifecycleEntrypoints) {
    J2EEAnalysisScope scope;
    if (exclusionsFile == null) {
      exclusionsFile = EXCLUSIONS_FILE;
    }
    scope = new J2EEAnalysisScope(BASIC_FILE, loader, exclusionsFile, lifecycleEntrypoints);
    for (int i = 0; i < J2SELibs.length; i++) {
      scope.addToScope(scope.getPrimordialLoader(), J2SELibs[i]);
    }
    for (int i = 0; i < J2EELibs.length; i++) {
      scope.addToScope(scope.getExtensionLoader(), J2EELibs[i]);
    }

    return scope;
  }

  public boolean useEJBLifecycleEntrypoints() {
    return lifecycleEntrypoints;
  }

  /**
   * Add each Module in application loader of the passed-in scope, to the
   * application loader of this scope.
   * 
   * 
   * @param scope
   *          an analysis scope.
   * @throws IllegalArgumentException  if scope is null
   */
  public void addToApplicationLoader(AnalysisScope scope) {
    if (scope == null) {
      throw new IllegalArgumentException("scope is null");
    }
    ClassLoaderReference app = scope.getApplicationLoader();
    for (Iterator it = scope.getModules(app).iterator(); it.hasNext();) {
      Module M = (Module) it.next();
      addToScope(getApplicationLoader(), M);
    }
  }
}
