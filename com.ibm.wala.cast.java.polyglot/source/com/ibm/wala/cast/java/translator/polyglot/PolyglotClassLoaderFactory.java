/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Oct 7, 2005
 */
package com.ibm.wala.cast.java.translator.polyglot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.ClassLoaderImpl;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.SetOfClasses;

public class PolyglotClassLoaderFactory extends ClassLoaderFactoryImpl {

  /**
   * A map from ClassLoaderReference to IRTranslatorExtension, so that source files in different languages are processed by the
   * right kind of IRTranslatorExtension.
   */
  final protected Map<ClassLoaderReference, IRTranslatorExtension> fExtensionMap = new HashMap<ClassLoaderReference, IRTranslatorExtension>();

  public PolyglotClassLoaderFactory(SetOfClasses exclusions, IRTranslatorExtension javaExtInfo) {
    super(exclusions);
    fExtensionMap.put(JavaSourceAnalysisScope.SOURCE, javaExtInfo);
  }

  protected IRTranslatorExtension getExtensionFor(ClassLoaderReference clr) {
    return fExtensionMap.get(clr);
  }

  @Override
  protected IClassLoader makeNewClassLoader(ClassLoaderReference classLoaderReference, IClassHierarchy cha, IClassLoader parent,
      AnalysisScope scope) throws IOException {
    if (classLoaderReference.equals(JavaSourceAnalysisScope.SOURCE)) {
      ClassLoaderImpl cl = new PolyglotSourceLoaderImpl(classLoaderReference, parent, getExclusions(), cha,
          getExtensionFor(classLoaderReference));
      cl.init(scope.getModules(classLoaderReference));
      return cl;
    } else {
      return super.makeNewClassLoader(classLoaderReference, cha, parent, scope);
    }
  }
}
