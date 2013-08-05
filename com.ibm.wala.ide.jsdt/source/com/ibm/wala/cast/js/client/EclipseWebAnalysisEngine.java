package com.ibm.wala.cast.js.client;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.ide.util.EclipseWebProjectPath;
import com.ibm.wala.ide.util.JavaScriptEclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;

public class EclipseWebAnalysisEngine extends EclipseJavaScriptAnalysisEngine {

  public EclipseWebAnalysisEngine(IJavaScriptProject project) throws IOException, CoreException {
    super(project);
  }

  @Override
  protected ClassLoaderFactory makeClassLoaderFactory(SetOfClasses exclusions) {
    return new WebPageLoaderFactory(new CAstRhinoTranslatorFactory());
  }

  @Override
  protected AnalysisScope makeAnalysisScope() {
    return new CAstAnalysisScope(new WebPageLoaderFactory(new CAstRhinoTranslatorFactory()), Collections.singleton(JavaScriptLoader.JS));
  }

  @Override
  protected JavaScriptEclipseProjectPath createProjectPath(IJavaScriptProject project) throws IOException, CoreException {
    return new EclipseWebProjectPath(project);
  }

  @Override
  public CallGraph getFieldBasedCallGraph(String scriptName) throws CancelException {
    Set<Entrypoint> eps= HashSetFactory.make();
    eps.add(JSCallGraphUtil.makeScriptRoots(getClassHierarchy()).make(scriptName));
    eps.add(JSCallGraphUtil.makeScriptRoots(getClassHierarchy()).make("Lpreamble.js"));
    eps.add(JSCallGraphUtil.makeScriptRoots(getClassHierarchy()).make("Lprologue.js"));
    return getFieldBasedCallGraph(eps);
  }

}
