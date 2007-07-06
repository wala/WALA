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

import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.ClassLoaderImpl;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

public class PolyglotClassLoaderFactory extends ClassLoaderFactoryImpl {

  final protected IRTranslatorExtension fExtInfo;

  public PolyglotClassLoaderFactory(SetOfClasses exclusions, IRTranslatorExtension extInfo) {
    super(exclusions);
    fExtInfo = extInfo;
  }

  protected IClassLoader makeNewClassLoader(ClassLoaderReference classLoaderReference, IClassHierarchy cha, IClassLoader parent,
      AnalysisScope scope) throws IOException {
    if (classLoaderReference.equals(EclipseProjectPath.SOURCE_REF)) {
      ClassLoaderImpl cl = new PolyglotSourceLoaderImpl(classLoaderReference, parent, getExclusions(), cha, fExtInfo);
      cl.init(scope.getModules(classLoaderReference));
      return cl;
    } else {
      return super.makeNewClassLoader(classLoaderReference, cha, parent, scope);
    }
  }
}
