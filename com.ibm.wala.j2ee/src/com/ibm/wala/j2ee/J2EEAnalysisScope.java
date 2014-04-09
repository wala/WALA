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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.jar.JarFile;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.io.FileProvider;

/**
 * Description of analysis for EJBs
 */
public class J2EEAnalysisScope extends AnalysisScope {

  private final static String BASIC_FILE = "SyntheticContainerModel.xml";

  private final static String DEFAULT_FILE = "DefaultWebsphereModules.xml";

  private final static String EXCLUSIONS_FILE = "J2EEClassHierarchyExclusions.txt";

  private final boolean lifecycleEntrypoints;

  static {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  /**
   * @param lifecycleEntrypoints Should EJB lifecycle entrypoints be considered as call graph entrypoints?
   */
  public J2EEAnalysisScope(String baseScope, ClassLoader loader, boolean lifecycleEntrypoints) throws IOException {
    this(baseScope, loader, (new FileProvider()).getFile(EXCLUSIONS_FILE), lifecycleEntrypoints);
  }

  /**
   * @param lifecycleEntrypoints Should EJB lifecycle entrypoints be considered as call graph entrypoints?
   */
  public J2EEAnalysisScope(String baseScope, ClassLoader loader, File exclusionsFile, boolean lifecycleEntrypoints)
      throws IOException {
    super(Collections.singleton(Language.JAVA));
    AnalysisScope base = AnalysisScopeReader.readJavaScope(baseScope, exclusionsFile, loader);

    for (ClassLoaderReference cl : base.getLoaders()) {
      for (Module m : base.getModules(cl)) {
        addToScope(cl, m);
      }
    }
    if (exclusionsFile != null) {
      FileOfClasses file = FileOfClasses.createFileOfClasses(exclusionsFile);
      setExclusions(file);
    }
    this.lifecycleEntrypoints = lifecycleEntrypoints;
  }

  /**
   * @param lifecycleEntrypoints Should EJB lifecycle entrypoints be considered as call graph entrypoints?
   */
  public static J2EEAnalysisScope makeDefault(ClassLoader loader, boolean lifecycleEntrypoints) throws IOException {
    return new J2EEAnalysisScope(DEFAULT_FILE, loader, lifecycleEntrypoints);
  }

  public static J2EEAnalysisScope make(JarFile[] J2SELibs, JarFile[] J2EELibs, ClassLoader loader, boolean lifecycleEntrypoints)
      throws IOException {
    return make(J2SELibs, J2EELibs, EXCLUSIONS_FILE, loader, lifecycleEntrypoints);
  }

  /**
   * @param lifecycleEntrypoints Should EJB lifecycle entrypoints be considered as call graph entrypoints?
   */
  public static J2EEAnalysisScope make(JarFile[] J2SELibs, JarFile[] J2EELibs, String exclusionsFile, ClassLoader loader,
      boolean lifecycleEntrypoints) throws IOException {
    J2EEAnalysisScope scope;
    scope = new J2EEAnalysisScope(BASIC_FILE, loader, new File(exclusionsFile), lifecycleEntrypoints);
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
   * @param lifecycleEntrypoints Should EJB lifecycle entrypoints be considered as call graph entrypoints?
   */
  public static J2EEAnalysisScope make(Module[] J2SELibs, Module[] J2EELibs, String exclusionsFile, ClassLoader loader,
      boolean lifecycleEntrypoints) throws IOException {
    J2EEAnalysisScope scope;
    if (exclusionsFile == null) {
      exclusionsFile = EXCLUSIONS_FILE;
    }
    scope = new J2EEAnalysisScope(BASIC_FILE, loader, new File(exclusionsFile), lifecycleEntrypoints);
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
   * Add each Module in application loader of the passed-in scope, to the application loader of this scope.
   * 
   * @param scope an analysis scope.
   * @throws IllegalArgumentException if scope is null
   */
  public void addToApplicationLoader(AnalysisScope scope) {
    if (scope == null) {
      throw new IllegalArgumentException("scope is null");
    }
    ClassLoaderReference app = scope.getApplicationLoader();
    for (Module m : scope.getModules(app)) {
      addToScope(getApplicationLoader(), m);
    }
  }
}
