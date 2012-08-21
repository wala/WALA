package com.ibm.wala.cast.java.client.polyglot;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.ide.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;

public class EclipseJavaScriptAnalysisEngine extends EclipseProjectSourceAnalysisEngine<IJavaScriptProject> {

  public EclipseJavaScriptAnalysisEngine(IJavaScriptProject project) throws IOException, CoreException {
    super(project, "js");
  }

  @Override
  protected AnalysisScope makeSourceAnalysisScope() {
    return new CAstAnalysisScope(new JavaScriptLoaderFactory(new CAstRhinoTranslatorFactory()), Collections.singleton(JavaScriptLoader.JS));
  }

  @Override
  protected EclipseProjectPath createProjectPath(IJavaScriptProject project) throws IOException, CoreException {
    return EclipseProjectPath.make(project);
  }

}
