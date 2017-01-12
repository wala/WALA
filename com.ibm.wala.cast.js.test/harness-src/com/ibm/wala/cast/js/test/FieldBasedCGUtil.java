/******************************************************************************
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.callgraph.fieldbased.FieldBasedCallGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.OptimisticCallgraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.PessimisticCallGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.WorklistBasedOptimisticCallgraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.ObjectVertex;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.util.Util;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.functions.Function;

/**
 * Utility class for building call graphs.
 * 
 * @author mschaefer
 *
 */
public class FieldBasedCGUtil {
	public static enum BuilderType { PESSIMISTIC, OPTIMISTIC, OPTIMISTIC_WORKLIST };

	private final JavaScriptTranslatorFactory translatorFactory;

	public FieldBasedCGUtil(JavaScriptTranslatorFactory translatorFactory) {
		this.translatorFactory = translatorFactory;
	}

  public Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> buildCG(URL url, BuilderType builderType, boolean supportFullPointerAnalysis, Function<Void, JSSourceExtractor> fExtractor) throws IOException, WalaException, CancelException  {
    return buildCG(url, builderType, new NullProgressMonitor(), supportFullPointerAnalysis, fExtractor);
  }

  public Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> buildCG(URL url, BuilderType builderType, IProgressMonitor monitor, boolean supportFullPointerAnalysis, Function<Void, JSSourceExtractor> fExtractor) throws IOException, WalaException, CancelException  {
    if (url.getFile().endsWith(".js")) {
      return buildScriptCG(url, builderType, monitor, supportFullPointerAnalysis);
    } else {
      return buildPageCG(url, builderType, monitor, supportFullPointerAnalysis, fExtractor);      
    }
  }
  
	public Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> buildScriptCG(URL url, BuilderType builderType, IProgressMonitor monitor, boolean supportFullPointerAnalysis) throws IOException, WalaException, CancelException  {
    JavaScriptLoaderFactory loaders = new JavaScriptLoaderFactory(translatorFactory);
    SourceModule[] scripts = new SourceModule[]{
        new SourceURLModule(url),
        JSCallGraphBuilderUtil.getPrologueFile("prologue.js")
    };
    return buildCG(loaders, scripts, builderType, monitor, supportFullPointerAnalysis);
	}

	 public Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> buildTestCG(String dir, String name, BuilderType builderType, IProgressMonitor monitor, boolean supportFullPointerAnalysis) throws IOException, WalaException, CancelException  {
	    JavaScriptLoaderFactory loaders = new JavaScriptLoaderFactory(translatorFactory);
	    SourceModule[] scripts = JSCallGraphBuilderUtil.makeSourceModules(dir, name);
	    return buildCG(loaders, scripts, builderType, monitor, supportFullPointerAnalysis);
	  }

	public Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> buildPageCG(URL url, BuilderType builderType, IProgressMonitor monitor, boolean supportFullPointerAnalysis, Function<Void, JSSourceExtractor> fExtractor) throws IOException, WalaException, CancelException  {
	  JavaScriptLoaderFactory loaders = new WebPageLoaderFactory(translatorFactory);
	  SourceModule[] scripts = JSCallGraphBuilderUtil.makeHtmlScope(url, loaders, fExtractor);
	  return buildCG(loaders, scripts, builderType, monitor, supportFullPointerAnalysis);
	}

	public Pair<JSCallGraph, PointerAnalysis<ObjectVertex>> buildCG(JavaScriptLoaderFactory loaders, SourceModule[] scripts, BuilderType builderType, IProgressMonitor monitor, boolean supportFullPointerAnalysis) throws IOException, WalaException, CancelException  {
		CAstAnalysisScope scope = new CAstAnalysisScope(scripts, loaders, Collections.singleton(JavaScriptLoader.JS));
		IClassHierarchy cha = ClassHierarchyFactory.make(scope, loaders, JavaScriptLoader.JS);
		Util.checkForFrontEndErrors(cha);
		Iterable<Entrypoint> roots = JSCallGraphUtil.makeScriptRoots(cha);
		FieldBasedCallGraphBuilder builder = null;
		
		AnalysisCache cache = new AnalysisCache(AstIRFactory.makeDefaultFactory());
		switch(builderType) {
		case PESSIMISTIC:
			builder = new PessimisticCallGraphBuilder(cha, JSCallGraphUtil.makeOptions(scope, cha, roots), cache, supportFullPointerAnalysis);
			break;
		case OPTIMISTIC:
			builder = new OptimisticCallgraphBuilder(cha, JSCallGraphUtil.makeOptions(scope, cha, roots), cache, supportFullPointerAnalysis);
			break;
		case OPTIMISTIC_WORKLIST:
		  builder = new WorklistBasedOptimisticCallgraphBuilder(cha, JSCallGraphUtil.makeOptions(scope, cha, roots), cache, supportFullPointerAnalysis);
		  break;
		}
		
		return builder.buildCallGraph(roots, monitor);
	}

	/*
	private JavaScriptLoaderFactory makeLoaderFactory(URL url) {
		return url.getFile().endsWith(".js") ? new JavaScriptLoaderFactory(translatorFactory) : new WebPageLoaderFactory(translatorFactory);
	}
	*/
	
	@SuppressWarnings("unused")
  private static void compareCGs(Map<String, Set<String>> cg1, Map<String, Set<String>> cg2) {
	  boolean diff = false;
	  for(String key : cg1.keySet()) {
	    Set<String> targets1 = cg1.get(key), targets2 = cg2.get(key);
	    if(targets2 == null) {
	      diff = true;
	      System.err.println("CG2 doesn't have call site" + key);
	    } else {
	      for(String target : targets1)
	        if(!targets2.contains(target)) {
	          diff = true;
	          System.err.println("CG2 doesn't have edge " + key + " -> " + target);
	        }
	      for(String target : targets2)
	        if(!targets1.contains(target)) {
	          diff = true;
	          System.err.println("CG1 doesn't have edge " + key + " -> " + target);
	        }
	    }
	  }
	  for(String key : cg2.keySet()) {
	    if(!cg1.containsKey(key)) {
        diff = true;
	      System.err.println("CG1 doesn't have call site " + key);
	    }
	  }
	  if(!diff)
	    System.err.println("call graphs are identical");
	}
}
