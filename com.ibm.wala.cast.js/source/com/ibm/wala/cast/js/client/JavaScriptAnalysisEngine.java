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
package com.ibm.wala.cast.js.client;

import java.io.IOException;
import java.util.Collections;
import java.util.jar.JarFile;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptEntryPoints;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.debug.Assertions;

public class JavaScriptAnalysisEngine extends AbstractAnalysisEngine {

  protected JavaScriptLoaderFactory loaderFactory;

  protected JavaScriptTranslatorFactory translatorFactory;

   public JavaScriptAnalysisEngine() {
  }

  @Override
  public void buildAnalysisScope() {
    try {
      loaderFactory = new JavaScriptLoaderFactory(translatorFactory);

      SourceModule[] files = moduleFiles.toArray(new SourceModule[moduleFiles.size()]);

      scope = new CAstAnalysisScope(files, loaderFactory, Collections.singleton(JavaScriptLoader.JS));
    } catch (IOException e) {
      Assertions.UNREACHABLE(e.toString());
    }
  }

  @Override
  public IClassHierarchy buildClassHierarchy() {
    try {
      return ClassHierarchy.make(getScope(), loaderFactory, JavaScriptLoader.JS);
    } catch (ClassHierarchyException e) {
      Assertions.UNREACHABLE(e.toString());
      return null;
    }
  }

  public void setTranslatorFactory(JavaScriptTranslatorFactory factory) {
    this.translatorFactory = factory;
  }

  @Override
  public void setJ2SELibraries(JarFile[] libs) {
    Assertions.UNREACHABLE("Illegal to call setJ2SELibraries");
  }

  @Override
  public void setJ2SELibraries(Module[] libs) {
    Assertions.UNREACHABLE("Illegal to call setJ2SELibraries");
  }

  @Override
  protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
    return new JavaScriptEntryPoints(cha, cha.getLoader(JavaScriptTypes.jsLoader));
  }

  @Override
  public AnalysisCache makeDefaultCache() {
    return new AnalysisCache(AstIRFactory.makeDefaultFactory());
  }

  @Override
  public AnalysisOptions getDefaultOptions(Iterable<Entrypoint> roots) {
    final AnalysisOptions options = new AnalysisOptions(scope, roots);

    options.setUseConstantSpecificKeys(true);

    options.setUseStacksForLexicalScoping(true);

    return options;
  }

  @Override
  protected CallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
    return new ZeroCFABuilderFactory().make((JSAnalysisOptions) options, cache, cha, scope, false);
  }
}
