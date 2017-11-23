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
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.callgraph.fieldbased.FieldBasedCallGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.OptimisticCallgraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.PessimisticCallGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FilteredFlowGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraph;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.ObjectVertex;
import com.ibm.wala.cast.js.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.js.html.IncludedPosition;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ide.client.EclipseProjectSourceAnalysisEngine;
import com.ibm.wala.ide.util.JavaScriptEclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.SetOfClasses;

public class EclipseJavaScriptAnalysisEngine<I extends InstanceKey> extends EclipseProjectSourceAnalysisEngine<IJavaScriptProject, I> {

  public enum BuilderType { PESSIMISTIC, OPTIMISTIC, REFLECTIVE }
  
  private final BuilderType builderType;
  
  public EclipseJavaScriptAnalysisEngine(IJavaScriptProject project, BuilderType builderType) {
    super(project, "js");
    this.builderType = builderType;
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
    return JavaScriptEclipseProjectPath.make(project, Collections.<Pair<String,Plugin>>emptySet());
  }

  @Override
  protected ClassLoaderReference getSourceLoader() {
	return JavaScriptTypes.jsLoader;
  }

  @Override
  public IAnalysisCacheView makeDefaultCache() {
    return new AnalysisCacheImpl(AstIRFactory.makeDefaultFactory());
  }

  @Override
  protected CallGraphBuilder<I> getCallGraphBuilder(IClassHierarchy cha,
		AnalysisOptions options, IAnalysisCacheView cache) {
	    return new ZeroCFABuilderFactory().make((JSAnalysisOptions)options, cache, cha);
  }

  public Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> getFieldBasedCallGraph() throws CancelException {
    return getFieldBasedCallGraph(JSCallGraphUtil.makeScriptRoots(getClassHierarchy()));
  }

  public Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> getFieldBasedCallGraph(String scriptName) throws CancelException {
    Set<Entrypoint> eps= HashSetFactory.make();
    eps.add(JSCallGraphUtil.makeScriptRoots(getClassHierarchy()).make(scriptName));
    eps.add(JSCallGraphUtil.makeScriptRoots(getClassHierarchy()).make("Lprologue.js"));
    return getFieldBasedCallGraph(eps);
  }
  
  private static String getScriptName(AstMethod m) {
    
    // we want the original including file, since that will be the "script"
    Position p = m.getSourcePosition();
    while (p instanceof IncludedPosition) {
      p = ((IncludedPosition)p).getIncludePosition();
    }
    
    String fileName = p.getURL().getFile();
    return fileName.substring(fileName.lastIndexOf('/') + 1);    
  }
  
  protected Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> getFieldBasedCallGraph(Iterable<Entrypoint> roots) throws CancelException {
    final Set<String> scripts = HashSetFactory.make();
    for(Entrypoint e : roots) {
      String scriptName = getScriptName(((AstMethod)e.getMethod()));
      scripts.add(scriptName);
    }
 
    final Function<IMethod, Boolean> filter = object -> {
      if (object instanceof AstMethod) {
         return scripts.contains(getScriptName((AstMethod)object));
      } else {
        return true;
      }
    };

    AnalysisOptions options = getDefaultOptions(roots);
    if (builderType.equals(BuilderType.OPTIMISTIC)) {
      ((JSAnalysisOptions)options).setHandleCallApply(false);
    }

    FieldBasedCallGraphBuilder builder = 
        builderType.equals(BuilderType.PESSIMISTIC)? 
            new PessimisticCallGraphBuilder(getClassHierarchy(), options, makeDefaultCache(), false) {
              @Override
              protected FlowGraph flowGraphFactory() {
                FlowGraphBuilder b = new FilteredFlowGraphBuilder(cha, cache, true, filter);
                return b.buildFlowGraph();
              }
              @Override
              protected boolean filterFunction(IMethod function) {
                 return super.filterFunction(function) && filter.apply(function);
              }     
            }      
            : new OptimisticCallgraphBuilder(getClassHierarchy(), options, makeDefaultCache(), true) {
              @Override
              protected FlowGraph flowGraphFactory() {
                FlowGraphBuilder b = new FilteredFlowGraphBuilder(cha, cache, true, filter);
                return b.buildFlowGraph();
              }  
            };
    
    return builder.buildCallGraph(roots, new NullProgressMonitor());
  }
}
