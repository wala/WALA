package com.ibm.wala.cast.js.client;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.ide.client.EclipseProjectSourceAnalysisEngine;
import com.ibm.wala.ide.util.JavaScriptEclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

public class EclipseJavaScriptAnalysisEngine extends EclipseProjectSourceAnalysisEngine<IJavaScriptProject> {

  public EclipseJavaScriptAnalysisEngine(IJavaScriptProject project) throws IOException, CoreException {
    super(project, "js");
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
	// TODO Auto-generated method stub
	return null;
  }

}
