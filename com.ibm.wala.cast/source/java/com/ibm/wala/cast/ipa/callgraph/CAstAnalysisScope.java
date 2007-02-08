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
// Licensed Materials - Property of IBM
// 5724-D15
// (C) Copyright IBM Corporation 2004. All Rights Reserved. 
// Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                             
// --------------------------------------------------------------------------- 

package com.ibm.wala.cast.ipa.callgraph;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import com.ibm.wala.cast.loader.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;

public class CAstAnalysisScope extends AnalysisScope {
  private final ClassLoaderReference theLoader;

  public CAstAnalysisScope(SingleClassLoaderFactory loaders) {
    this.theLoader = loaders.getTheReference();
  }

  public CAstAnalysisScope(String[] sourceFileNames, SingleClassLoaderFactory loaders) throws IOException {
    this(loaders);
    for(int i = 0; i < sourceFileNames.length; i++) {
      File F = new File( sourceFileNames[i] );
      addSourceFileToScope(theLoader, F, F.getParent());
    }
  }
    
  public CAstAnalysisScope(URL[] sources, SingleClassLoaderFactory loaders) throws IOException {
    this(loaders);
    for(int i = 0; i < sources.length; i++) {
      addToScope(theLoader, new SourceURLModule(sources[i]));
    }
  }
    
  public CAstAnalysisScope(SourceFileModule[] sources, SingleClassLoaderFactory loaders) throws IOException {
    this(loaders);
    for(int i = 0; i < sources.length; i++) {
      addToScope(theLoader, sources[i]);
    }
  }
    
  /**
   * Return the information regarding the primordial loader.
   * 
   * @return ClassLoaderReference
   */
  public ClassLoaderReference getPrimordialLoader() {
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * Return the information regarding the extension loader.
   * 
   * @return ClassLoaderReference
   */
  public ClassLoaderReference getExtensionLoader() {
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * Return the information regarding the application loader.
   * 
   * @return ClassLoaderReference
   */
  public ClassLoaderReference getApplicationLoader() {
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * @return Returns the arrayClassLoader.
   */
  public ArrayClassLoader getArrayClassLoader() {
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * Return the information regarding the application loader.
   * 
   * @return ClassLoaderReference
   */
  public ClassLoaderReference getSyntheticLoader() {
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * Add a class file to the scope for a loader
   * 
   * @param loader
   * @param file
   */
  public void addClassFileToScope(ClassLoaderReference loader, File file) {
    Assertions.UNREACHABLE();
  }
  
  /**
   * @return the ClassLoaderReference specified by <code>name</code>.
   */
  public ClassLoaderReference getLoader(Atom name) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(name.equals(theLoader.getName()));
    }
    return theLoader;
  }

  /**
   * @return an Iterator <ClassLoaderReference>over the loaders.
   */
  public Iterator<ClassLoaderReference> getLoaders() {
    return new NonNullSingletonIterator<ClassLoaderReference>( theLoader );
  }

  /**
   * @return the number of loaders.
   */
  public int getNumberOfLoaders() {
    return 1;
  }

}
