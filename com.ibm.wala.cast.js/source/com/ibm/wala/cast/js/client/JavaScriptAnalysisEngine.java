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
import java.util.jar.JarFile;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptEntryPoints;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.client.impl.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.debug.Assertions;

public class JavaScriptAnalysisEngine extends AbstractAnalysisEngine {

  protected JavaScriptLoaderFactory loaderFactory;

  protected JavaScriptTranslatorFactory translatorFactory;

  protected boolean keepIRs = true;

  public JavaScriptAnalysisEngine() {
    setCallGraphBuilderFactory(new com.ibm.wala.cast.js.client.impl.ZeroCFABuilderFactory());
  }

  @SuppressWarnings("unchecked")
  protected void buildAnalysisScope() {
    try {
      if (translatorFactory == null) {
        translatorFactory = new JavaScriptTranslatorFactory.CAstRhinoFactory();
      }

      loaderFactory = new JavaScriptLoaderFactory(translatorFactory);

      SourceFileModule[] files = (SourceFileModule[]) moduleFiles.toArray(new SourceFileModule[moduleFiles.size()]);

      scope = new CAstAnalysisScope(files, loaderFactory);
    } catch (IOException e) {
      Assertions.UNREACHABLE(e.toString());
    }
  }

  protected IClassHierarchy buildClassHierarchy() {
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

  public void setJ2SELibraries(JarFile[] libs) {
    Assertions.UNREACHABLE("Illegal to call setJ2SELibraries");
  }

  public void setJ2SELibraries(Module[] libs) {
    Assertions.UNREACHABLE("Illegal to call setJ2SELibraries");
  }

  protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
    return new JavaScriptEntryPoints(cha, cha.getLoader(JavaScriptTypes.jsLoader));
  }

  @Override
  public AnalysisCache makeDefaultCache() {
    return new AnalysisCache(AstIRFactory.makeDefaultFactory(keepIRs));
  }

  public AnalysisOptions getDefaultOptions(Iterable<Entrypoint> roots) {
    final AnalysisOptions options = new AnalysisOptions(scope, roots);

    options.setUseConstantSpecificKeys(true);

    options.setUseStacksForLexicalScoping(true);

    options.getSSAOptions().setPreserveNames(true);

    return options;
  }
}
