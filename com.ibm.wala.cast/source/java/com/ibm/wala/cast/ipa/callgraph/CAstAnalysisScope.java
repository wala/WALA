/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package com.ibm.wala.cast.ipa.callgraph;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.cast.loader.SingleClassLoaderFactory;
import com.ibm.wala.classLoader.ArrayClassLoader;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

public class CAstAnalysisScope extends AnalysisScope {
  private final ClassLoaderReference theLoader;

  public CAstAnalysisScope(SingleClassLoaderFactory loaders, Collection<Language> languages) {
    super(languages);
    this.theLoader = loaders.getTheReference();
  }

  public CAstAnalysisScope(String[] sourceFileNames, SingleClassLoaderFactory loaders, Collection<Language> languages)
      throws IOException {
    this(loaders, languages);
    for (int i = 0; i < sourceFileNames.length; i++) {
      File F = new File(sourceFileNames[i]);
      addSourceFileToScope(theLoader, F, F.getPath());
    }
  }

  public CAstAnalysisScope(URL[] sources, SingleClassLoaderFactory loaders, Collection<Language> languages) throws IOException {
    this(loaders, languages);
    for (int i = 0; i < sources.length; i++) {
      addToScope(theLoader, new SourceURLModule(sources[i]));
    }
  }

  public CAstAnalysisScope(SourceModule[] sources, SingleClassLoaderFactory loaders, Collection<Language> languages)
      throws IOException {
    this(loaders, languages);
    for (int i = 0; i < sources.length; i++) {
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
    assert name.equals(theLoader.getName());
    return theLoader;
  }

  /**
   */
  public Collection<ClassLoaderReference> getLoaders() {
    return Collections.singleton(theLoader);
  }

  /**
   * @return the number of loaders.
   */
  public int getNumberOfLoaders() {
    return 1;
  }

  public String toString() {
    return super.toString() + "\n" + theLoader + ": " + getModules(theLoader);
  }
}
