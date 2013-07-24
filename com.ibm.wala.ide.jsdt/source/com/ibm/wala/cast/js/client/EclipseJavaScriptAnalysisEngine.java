/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.client;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.callgraph.fieldbased.FieldBasedCallGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.PessimisticCallGraphBuilder;
import com.ibm.wala.cast.js.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.ide.client.EclipseProjectSourceAnalysisEngine;
import com.ibm.wala.ide.util.JavaScriptEclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;

public class EclipseJavaScriptAnalysisEngine extends EclipseProjectSourceAnalysisEngine<IJavaScriptProject> {

  public EclipseJavaScriptAnalysisEngine(IJavaScriptProject project) throws IOException, CoreException {
    super(project, "js");
  }

  
  @Override
  public AnalysisOptions getDefaultOptions(Iterable<Entrypoint> entrypoints) {
	return JSCallGraphUtil.makeOptions(getScope(), getClassHierarchy(), entrypoints);
  }

  @Override
  public String getExclusionsFile() {
	  return null;
  }

  
  @Override
  protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
	return JSCallGraphUtil.makeScriptRoots(cha);
  }

@Override
  protected ClassLoaderFactory makeClassLoaderFactory(SetOfClasses exclusions) {
	return JSCallGraphUtil.makeLoaders();
  }

@Override
  protected AnalysisScope makeAnalysisScope() {
    return new CAstAnalysisScope(new JavaScriptLoaderFactory(new CAstRhinoTranslatorFactory()), Collections.singleton(JavaScriptLoader.JS));
  }

  @Override
  protected JavaScriptEclipseProjectPath createProjectPath(IJavaScriptProject project) throws IOException, CoreException {
    return JavaScriptEclipseProjectPath.make(project);
  }

  @Override
  protected ClassLoaderReference getSourceLoader() {
	return JavaScriptTypes.jsLoader;
  }

  @Override
  public AnalysisCache makeDefaultCache() {
    return new AnalysisCache(AstIRFactory.makeDefaultFactory());
  }

  @Override
  protected CallGraphBuilder getCallGraphBuilder(IClassHierarchy cha,
		AnalysisOptions options, AnalysisCache cache) {
	    return new ZeroCFABuilderFactory().make((JSAnalysisOptions)options, cache, cha, scope, false);
  }

  public CallGraph getFieldBasedCallGraph() throws CancelException {
    Iterable<Entrypoint> roots = JSCallGraphUtil.makeScriptRoots(getClassHierarchy());
    FieldBasedCallGraphBuilder builder = new PessimisticCallGraphBuilder(getClassHierarchy(), getDefaultOptions(roots), makeDefaultCache());
    return builder.buildCallGraph(new NullProgressMonitor());
  }
}
