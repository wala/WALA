/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.cha;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy.Node;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public class ClassHierarchyFactory {

  /**
   * @return a ClassHierarchy object representing the analysis scope
   * @throws ClassHierarchyException
   */
  public static ClassHierarchy make(AnalysisScope scope) throws ClassHierarchyException {
    if (scope == null) {
      throw new IllegalArgumentException("null scope");
    }
    return make(scope, new ClassLoaderFactoryImpl(scope.getExclusions()));
  }

  /**
   * temporarily marking this internal to avoid infinite sleep with randomly chosen IProgressMonitor.
   */
  public static ClassHierarchy make(AnalysisScope scope, IProgressMonitor monitor) throws ClassHierarchyException {
    if (scope == null) {
      throw new IllegalArgumentException("null scope");
    }
    return make(scope, new ClassLoaderFactoryImpl(scope.getExclusions()), monitor);
  }

  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory) throws ClassHierarchyException {
    if (scope == null) {
      throw new IllegalArgumentException("null scope");
    }
    if (factory == null) {
      throw new IllegalArgumentException("null factory");
    }
    return new ClassHierarchy(scope, factory, null, new ConcurrentHashMap<TypeReference, Node>());
  }

  /**
   * temporarily marking this internal to avoid infinite sleep with randomly chosen IProgressMonitor.
   */
  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, IProgressMonitor monitor)
      throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, monitor, new ConcurrentHashMap<TypeReference, Node>());
  }

  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, Set<Language> languages)
      throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, languages, null, new ConcurrentHashMap<TypeReference, Node>());
  }

  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, Language language)
      throws ClassHierarchyException {
    return new ClassHierarchy(scope, factory, language, null, new ConcurrentHashMap<TypeReference, Node>());
  }

  /**
   * temporarily marking this internal to avoid infinite sleep with randomly chosen IProgressMonitor. TODO: nanny for testgen
   */
  public static ClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory, Language language, IProgressMonitor monitor)
      throws ClassHierarchyException {
    if (factory == null) {
      throw new IllegalArgumentException("null factory");
    }
    return new ClassHierarchy(scope, factory, language, monitor, new ConcurrentHashMap<TypeReference, Node>());
  }

}
