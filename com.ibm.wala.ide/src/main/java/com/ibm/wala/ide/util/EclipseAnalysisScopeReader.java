/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ide.util;

import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ide.plugin.CorePlugin;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.Plugin;

public class EclipseAnalysisScopeReader extends AnalysisScopeReader {

  public AnalysisScope readJavaScopeFromPlugin(
      String scopeFileName, File exclusionsFile, ClassLoader javaLoader) throws IOException {
    return readJavaScopeFromPlugin(
        scopeFileName, exclusionsFile, javaLoader, CorePlugin.getDefault());
  }

  public AnalysisScope readJavaScopeFromPlugin(
      String scopeFileName,
      File exclusionsFile,
      ClassLoader javaLoader,
      @SuppressWarnings("unused") Plugin plugIn)
      throws IOException {
    AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
    return read(scope, scopeFileName, exclusionsFile, javaLoader);
  }

  public AnalysisScope makePrimordialScopeFromPlugin(File exclusionsFile) throws IOException {
    return makePrimordialScopeFromPlugin(exclusionsFile, CorePlugin.getDefault());
  }
  /**
   * @param exclusionsFile file holding class hierarchy exclusions. may be null
   * @throws IllegalStateException if there are problmes reading wala properties
   */
  public AnalysisScope makePrimordialScopeFromPlugin(
      File exclusionsFile, @SuppressWarnings("unused") Plugin plugIn) throws IOException {
    return read(
        AnalysisScope.createJavaAnalysisScope(),
        BASIC_FILE,
        exclusionsFile,
        EclipseAnalysisScopeReader.class.getClassLoader());
  }

  public AnalysisScope makeJavaBinaryAnalysisScopeFromPlugin(String classPath, File exclusionsFile)
      throws IOException {
    return makeJavaBinaryAnalysisScopeFromPlugin(
        classPath, exclusionsFile, CorePlugin.getDefault());
  }

  /**
   * @param classPath class path to analyze, delimited by File.pathSeparator
   * @param exclusionsFile file holding class hierarchy exclusions. may be null
   * @throws IllegalStateException if there are problems reading wala properties
   */
  public AnalysisScope makeJavaBinaryAnalysisScopeFromPlugin(
      String classPath, File exclusionsFile, Plugin plugIn) throws IOException {
    if (classPath == null) {
      throw new IllegalArgumentException("classPath null");
    }
    AnalysisScope scope = makePrimordialScopeFromPlugin(exclusionsFile, plugIn);
    ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);

    addClassPathToScope(classPath, scope, loader);

    return scope;
  }
}
