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
package com.ibm.wala.cast.java.test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.java.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.translator.polyglot.IRTranslatorExtension;
import com.ibm.wala.cast.java.translator.polyglot.JavaIRTranslatorExtension;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.client.impl.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.Value;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.XMLSetOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * @author julian dolby?
 * @author sfink. refactored to clean up eclipse utilities.
 * 
 */
public class TestSourceAnalysisEngine extends AbstractAnalysisEngine {

  /**
   * Modules which are user-space code
   */
  private final Set<Module> userEntries = new HashSet<Module>();

  /**
   * Modules which are source code
   */
  private final Set<Module> sourceEntries = new HashSet<Module>();

  /**
   * Modules which are system or library code TODO: what about extension loader?
   */
  private final Set<Module> systemEntries = new HashSet<Module>();

  public TestSourceAnalysisEngine() {
    super();
    setCallGraphBuilderFactory(new ZeroCFABuilderFactory());
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

  protected void addApplicationModulesToScope() {
    ClassLoaderReference app = scope.getApplicationLoader();
    for (Iterator it = userEntries.iterator(); it.hasNext();) {
      Module M = (Module) it.next();
      scope.addToScope(app, M);
    }

    ClassLoaderReference src = ((JavaSourceAnalysisScope) scope).getSourceLoader();

    for (Iterator it = sourceEntries.iterator(); it.hasNext();) {
      Module M = (Module) it.next();
      scope.addToScope(src, M);
    }
  }

  protected void buildAnalysisScope() {
    scope = new JavaSourceAnalysisScope();

    if (getExclusionsFile() != null) {
      ClassLoader loader = getClass().getClassLoader();
      scope.setExclusions(new XMLSetOfClasses(getExclusionsFile(), loader));
    }

    for (Iterator modules = systemEntries.iterator(); modules.hasNext();) {
      scope.addToScope(scope.getPrimordialLoader(), (Module) modules.next());
    }

    // add user stuff
    addApplicationModulesToScope();
  }

  public IRTranslatorExtension getTranslatorExtension() {
    return new JavaIRTranslatorExtension();
  }

  protected ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions, WarningSet warnings, IRTranslatorExtension extInfo) {
    return new PolyglotClassLoaderFactory(exclusions, warnings, extInfo);
  }

  protected ClassHierarchy buildClassHierarchy() {
    ClassHierarchy cha = null;
    ClassLoaderFactory factory = getClassLoaderFactory(scope.getExclusions(), getWarnings(), getTranslatorExtension());

    try {
      cha = ClassHierarchy.make(getScope(), factory, getWarnings());
    } catch (ClassHierarchyException e) {
      System.err.println("Class Hierarchy construction failed");
      System.err.println(e.toString());
      e.printStackTrace();
    }
    return cha;
  }

  protected Entrypoints makeDefaultEntrypoints(AnalysisScope scope, ClassHierarchy cha) {
    return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE_REF, cha);
  }

  public AnalysisOptions getDefaultOptions(Entrypoints entrypoints) {
    AnalysisOptions options = new AnalysisOptions(getScope(), AstIRFactory.makeDefaultFactory(true), entrypoints);

    SSAOptions ssaOptions = new SSAOptions();
    ssaOptions.setDefaultValues(new SSAOptions.DefaultValues() {
      public int getDefaultValue(SymbolTable symtab, int valueNumber) {
        Value v = symtab.getValue(valueNumber);
        if (v == null) {
          Assertions._assert(v != null, "no default for " + valueNumber);
        }
        return v.getDefaultValue(symtab);
      }
    });

    options.setSSAOptions(ssaOptions);

    return options;
  }
}
