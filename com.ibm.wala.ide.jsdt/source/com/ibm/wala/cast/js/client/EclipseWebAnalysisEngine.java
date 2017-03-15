package com.ibm.wala.cast.js.client;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.ObjectVertex;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.ide.jsdt.Activator;
import com.ibm.wala.ide.util.EclipseWebProjectPath;
import com.ibm.wala.ide.util.JavaScriptEclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.SetOfClasses;

public class EclipseWebAnalysisEngine extends EclipseJavaScriptAnalysisEngine<InstanceKey> {

  private final Set<Pair<String, Plugin>> models = HashSetFactory.make();
  
  public EclipseWebAnalysisEngine(IJavaScriptProject project, Collection<Pair<String, Plugin>> models, BuilderType builderType) throws IOException, CoreException {
    super(project, builderType);
    // core DOM model
    this.models.add(Pair.make("preamble.js", (Plugin)Activator.getDefault()));
    this.models.addAll(models);
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
    return EclipseWebProjectPath.make(project, models);
  }

  @Override
  public Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> getFieldBasedCallGraph(String scriptName) throws CancelException {
    Set<Entrypoint> eps= HashSetFactory.make();
    eps.add(JSCallGraphUtil.makeScriptRoots(getClassHierarchy()).make(scriptName));
    eps.add(JSCallGraphUtil.makeScriptRoots(getClassHierarchy()).make("Lprologue.js"));
    
    for(Pair<String,Plugin> model : models) {
      eps.add(JSCallGraphUtil.makeScriptRoots(getClassHierarchy()).make("L" + model.fst));
    }

    return getFieldBasedCallGraph(eps);
  }

}
