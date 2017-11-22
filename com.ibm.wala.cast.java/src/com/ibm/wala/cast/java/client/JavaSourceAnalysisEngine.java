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
package com.ibm.wala.cast.java.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.java.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.io.FileProvider;

/**
 */
public abstract class JavaSourceAnalysisEngine<I extends InstanceKey> extends AbstractAnalysisEngine<I> {

  /**
   * Modules which are user-space code
   */
  private final Set<Module> userEntries = HashSetFactory.make();

  /**
   * Modules which are source code
   */
  private final Set<Module> sourceEntries = HashSetFactory.make();

  /**
   * Modules which are system or library code TODO: what about extension loader?
   */
  private final Set<Module> systemEntries = HashSetFactory.make();

  public JavaSourceAnalysisEngine() {
    super();
  }

  /**
   * Adds the given source module to the source loader's module list. Clients
   * should/may call this method if they don't supply an IJavaProject to the
   * constructor.
   */
  public void addSourceModule(Module M) {
    sourceEntries.add(M);
  }

  /**
   * Adds the given compiled module to the application loader's module list.
   * Clients should/may call this method if they don't supply an IJavaProject to
   * the constructor.
   */
  public void addCompiledModule(Module M) {
    userEntries.add(M);
  }

  /**
   * Adds the given module to the primordial loader's module list. Clients
   * should/may call this method if they don't supply an IJavaProject to the
   * constructor.
   */
  public void addSystemModule(Module M) {
    systemEntries.add(M);
  }

  @Override
  protected void addApplicationModulesToScope() {
    ClassLoaderReference app = scope.getApplicationLoader();
    for (Module M : userEntries) {
      scope.addToScope(app, M);
    }

    ClassLoaderReference src = ((JavaSourceAnalysisScope) scope).getSourceLoader();

    for (Module M : sourceEntries) {
      scope.addToScope(src, M);
    }
  }

  @Override
  public void buildAnalysisScope() throws IOException {
    scope = makeSourceAnalysisScope();

    if (getExclusionsFile() != null) {
      try (final InputStream is = new File(getExclusionsFile()).exists()? new FileInputStream(getExclusionsFile()): FileProvider.class.getClassLoader().getResourceAsStream(getExclusionsFile())) {
        scope.setExclusions(new FileOfClasses(is));
      }
    }

    for (Module M : this.systemEntries) {
      scope.addToScope(scope.getPrimordialLoader(), M);
    }

    // add user stuff
    addApplicationModulesToScope();
  }

  protected AnalysisScope makeSourceAnalysisScope() {
    return new JavaSourceAnalysisScope();
  }

  protected abstract ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions);
  
  @Override
  public IClassHierarchy buildClassHierarchy() {
    IClassHierarchy cha = null;
    ClassLoaderFactory factory = getClassLoaderFactory(scope.getExclusions());

    try {
      cha = ClassHierarchyFactory.make(getScope(), factory);
    } catch (ClassHierarchyException e) {
      System.err.println("Class Hierarchy construction failed");
      System.err.println(e.toString());
      e.printStackTrace();
    }
    return cha;
  }

  @Override
  protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
    return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha);
  }

  @Override
  public IAnalysisCacheView makeDefaultCache() {
    return new AnalysisCacheImpl(AstIRFactory.makeDefaultFactory());
  }

  @Override
  public AnalysisOptions getDefaultOptions(Iterable<Entrypoint> entrypoints) {
    AnalysisOptions options = new AnalysisOptions(getScope(), entrypoints);

    SSAOptions ssaOptions = new SSAOptions();
    ssaOptions.setDefaultValues(SymbolTable::getDefaultValue);

    options.setSSAOptions(ssaOptions);

    return options;
  }

  @Override
  protected CallGraphBuilder<I> getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
    return new ZeroCFABuilderFactory().make(options, cache, cha, scope);
  }
}
