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
package com.ibm.wala.cast.js.test;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Set;

import junit.framework.Assert;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSZeroOrOneXCFABuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.util.CancelException;

/**
 * TODO this class is a mess.  rewrite.
 */
public class Util extends com.ibm.wala.cast.js.ipa.callgraph.Util {

  public static JSCFABuilder makeScriptCGBuilder(String dir, String name, boolean useOneCFA, IRFactory irFactory) throws IOException {
    JavaScriptLoaderFactory loaders = Util.makeLoaders();

    AnalysisScope scope = makeScriptScope(dir, name, loaders);

    return makeCG(loaders, scope, useOneCFA, true, irFactory);
  }

  static AnalysisScope makeScriptScope(String dir, String name, JavaScriptLoaderFactory loaders) throws IOException {
    URL script = Util.class.getClassLoader().getResource(dir + File.separator + name);
    if (script == null) {
      script = Util.class.getClassLoader().getResource(dir + "/" + name);
    }
    assert script != null : "cannot find " + dir + " and " + name;

    AnalysisScope scope;
    if (script.openConnection() instanceof JarURLConnection) {
      scope = makeScope(new URL[] { script }, loaders, JavaScriptLoader.JS);
    } else {
      scope = makeScope(new SourceFileModule[] { makeSourceModule(script, dir, name) }, loaders, JavaScriptLoader.JS);
    }
    return scope;
  }

  public static JSCFABuilder makeScriptCGBuilder(String dir, String name) throws IOException {
    return makeScriptCGBuilder(dir, name, false, AstIRFactory.makeDefaultFactory());
  }

  public static CallGraph makeScriptCG(String dir, String name) throws IOException, IllegalArgumentException, CancelException {
    return makeScriptCG(dir, name, AstIRFactory.makeDefaultFactory());
  }

  public static CallGraph makeScriptCG(String dir, String name, boolean useOneCFA) throws IOException, IllegalArgumentException, CancelException {
    return makeScriptCG(dir, name, useOneCFA, AstIRFactory.makeDefaultFactory());
  }

  public static CallGraph makeScriptCG(String dir, String name, IRFactory irFactory) throws IOException, IllegalArgumentException, CancelException {
    return makeScriptCG(dir, name, false, irFactory);
  }

  public static CallGraph makeScriptCG(String dir, String name, boolean useOneCFA, IRFactory irFactory) throws IOException, IllegalArgumentException,
      CancelException {
    PropagationCallGraphBuilder b = makeScriptCGBuilder(dir, name, useOneCFA, irFactory);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    dumpCG(b.getPointerAnalysis(), CG);
    return CG;
  }

  public static CallGraph makeScriptCG(SourceModule[] scripts, boolean useOneCFA, IRFactory irFactory) throws IOException, IllegalArgumentException,
      CancelException {
    PropagationCallGraphBuilder b = makeCGBuilder(makeLoaders(), scripts, useOneCFA, true, irFactory);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    dumpCG(b.getPointerAnalysis(), CG);
    return CG;
  }

  public static JSCFABuilder makeHTMLCGBuilder(URL url) throws IOException {
    return makeHTMLCGBuilder(url, true);
  }
  public static JSCFABuilder makeHTMLCGBuilder(URL url, boolean handleCallApply) throws IOException {
    JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
    Set<MappedSourceModule> script = WebUtil.extractScriptFromHTML(url);
    JSCFABuilder builder = makeCGBuilder(new WebPageLoaderFactory(translatorFactory), script.toArray(new SourceModule[script.size()]), false, handleCallApply, AstIRFactory.makeDefaultFactory());
    builder.setBaseURL(url);
    return builder;
  }

  public static CallGraph makeHTMLCG(URL url) throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder b = makeHTMLCGBuilder(url);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    dumpCG(b.getPointerAnalysis(), CG);
    return CG;
  }

  public static CallGraph makeHTMLCG(URL url, boolean handleCallApply) throws IOException, IllegalArgumentException,
      CancelException {
    PropagationCallGraphBuilder b = makeHTMLCGBuilder(url, handleCallApply);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    return CG;
  }

  public static JSCFABuilder makeCGBuilder(JavaScriptLoaderFactory loaders, SourceModule[] scripts, boolean useOneCFA, boolean handleCallApply, IRFactory irFactory) throws IOException {
    AnalysisScope scope = makeScope(scripts, loaders, JavaScriptLoader.JS);
    return makeCG(loaders, scope, useOneCFA, handleCallApply, irFactory);
  }

  protected static JSCFABuilder makeCG(JavaScriptLoaderFactory loaders, AnalysisScope scope, boolean useOneCFA, boolean handleCallApply, IRFactory irFactory) throws IOException {
    try {
      IClassHierarchy cha = makeHierarchy(scope, loaders);
      com.ibm.wala.cast.js.util.Util.checkForFrontEndErrors(cha);
      Iterable<Entrypoint> roots = makeScriptRoots(cha);
      JSAnalysisOptions options = makeOptions(scope, cha, roots);
      options.setHandleCallApply(handleCallApply);
      AnalysisCache cache = makeCache(irFactory);

      JSCFABuilder builder = new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, ZeroXInstanceKeys.ALLOCATIONS, useOneCFA);

      return builder;
    } catch (ClassHierarchyException e) {
      Assert.assertTrue("internal error building class hierarchy", false);
      return null;
    }
  }
}
