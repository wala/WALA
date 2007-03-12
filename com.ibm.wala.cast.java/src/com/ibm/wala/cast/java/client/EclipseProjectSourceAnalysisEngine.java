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

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jdt.core.*;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.java.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.translator.polyglot.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.client.impl.EclipseProjectAnalysisEngine;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.XMLSetOfClasses;

public class EclipseProjectSourceAnalysisEngine extends
  EclipseProjectAnalysisEngine 
{

  public EclipseProjectSourceAnalysisEngine(IJavaProject project) {
    super(project);
    setCallGraphBuilderFactory( new ZeroCFABuilderFactory() );
  }

  public EclipseProjectSourceAnalysisEngine() {
    super();
    setCallGraphBuilderFactory( new ZeroCFABuilderFactory() );
  }

  /**
   * Adds the given source module to the source loader's module list.
   * Clients should/may call this
   * method if they don't supply an IJavaProject to the constructor.
   */
  public void addSourceModule(Module M) {
    Assertions._assert(project == null);
    sourceEntries.add(M);
  }

  /**
   * Adds the given compiled module to the application loader's module list.
   * Clients should/may call this
   * method if they don't supply an IJavaProject to the constructor.
   */
  public void addCompiledModule(Module M) {
    Assertions._assert(project == null);
    userEntries.add(M);
  }

  /**
   * Adds the given module to the primordial loader's module list.
   * Clients should/may call this
   * method if they don't supply an IJavaProject to the constructor.
   */
  public void addSystemModule(Module M) {
    Assertions._assert(project == null);
    systemEntries.add(M);
  }

  protected void addApplicationModulesToScope() {
    ClassLoaderReference app = scope.getApplicationLoader();
    for (Iterator it = userEntries.iterator(); it.hasNext();) {
      Module M = (Module) it.next();
      scope.addToScope(app, M);
    }
    
    ClassLoaderReference src = 
      ((JavaSourceAnalysisScope) scope).getSourceLoader();

    for (Iterator it = sourceEntries.iterator(); it.hasNext();) {
      Module M = (Module) it.next();
      scope.addToScope(src, M);
    }
  }

  protected void buildAnalysisScope() {
    try {
      scope = new JavaSourceAnalysisScope();

      if (getExclusionsFile() != null) {
	ClassLoader loader = getClass().getClassLoader();
	scope.setExclusions(new XMLSetOfClasses(getExclusionsFile(), loader));
      }

      if (project != null) {
	resolveClasspathEntries(project.getRawClasspath(), true);
      }

      for (Iterator modules = systemEntries.iterator(); modules.hasNext();) {
	scope.addToScope(scope.getPrimordialLoader(), 
			 (Module) modules.next());
      }

      // add user stuff
      addApplicationModulesToScope();
    } catch (JavaModelException e) {
      Assertions.UNREACHABLE();
    } catch (IOException e) {
      Assertions.UNREACHABLE();
    }
  }

  public IRTranslatorExtension getTranslatorExtension() {
    return new JavaIRTranslatorExtension();
  }

  protected ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions, WarningSet warnings, IRTranslatorExtension extInfo) {
      return new PolyglotClassLoaderFactory(exclusions, warnings, extInfo);
  }

  protected ClassHierarchy buildClassHierarchy() {
    ClassHierarchy cha = null;
    ClassLoaderFactory factory = getClassLoaderFactory(scope.getExclusions(), 
	getWarnings(), getTranslatorExtension());
    
    try {
      cha = ClassHierarchy.make(getScope(), factory, getWarnings());
    } catch (ClassHierarchyException e) {
      System.err.println("Class Hierarchy construction failed");
      System.err.println(e.toString());
      e.printStackTrace();
    }
    return cha;
  }

  protected Entrypoints 
    makeDefaultEntrypoints(AnalysisScope scope, ClassHierarchy cha) 
  {
    return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE_REF, cha);
  }

  public AnalysisOptions getDefaultOptions(Entrypoints entrypoints) {
    return 
      new AnalysisOptions(
        getScope(), 
	AstIRFactory.makeDefaultFactory(true),
	entrypoints);
  }
}
