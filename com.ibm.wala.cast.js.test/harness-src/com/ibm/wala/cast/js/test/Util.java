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

import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSZeroOrOneXCFABuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;

public class Util extends com.ibm.wala.cast.js.ipa.callgraph.Util {

  public static JSCFABuilder makeScriptCGBuilder(String dir, String name, boolean useOneCFA) throws IOException {
    JavaScriptLoaderFactory loaders = Util.makeLoaders();

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

    return makeCG(loaders, scope, useOneCFA);
  }

  public static JSCFABuilder makeScriptCGBuilder(String dir, String name) throws IOException {
    return makeScriptCGBuilder(dir, name, false);
  }

  public static CallGraph makeScriptCG(String dir, String name) throws IOException, IllegalArgumentException, CancelException {
    return makeScriptCG(dir, name, false);
  }

  public static CallGraph makeScriptCG(String dir, String name, boolean useOneCFA) throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder b = makeScriptCGBuilder(dir, name, useOneCFA);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    dumpCG(b, CG);
    return CG;
  }

  public static CallGraph makeScriptCG(SourceModule[] scripts, boolean useOneCFA) throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder b = makeCGBuilder(scripts, useOneCFA);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    dumpCG(b, CG);
    return CG;
  }

  public static JSCFABuilder makeHTMLCGBuilder(URL url) throws IOException {
    JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
    Set<MappedSourceModule> script = WebUtil.extractScriptFromHTML(url);
    JSCFABuilder builder = makeCGBuilder(script.toArray(new SourceModule[script.size()]), false);
    builder.setBaseURL(url);
    return builder;
  }

  public static CallGraph makeHTMLCG(URL url) throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder b = makeHTMLCGBuilder(url);
    CallGraph CG = b.makeCallGraph(b.getOptions());
//    dumpCG(b, CG);
    return CG;
  }

  public static JSCFABuilder makeCGBuilder(SourceModule[] scripts, boolean useOneCFA) throws IOException {
    JavaScriptLoaderFactory loaders = makeLoaders();
    AnalysisScope scope = makeScope(scripts, loaders, JavaScriptLoader.JS);
    return makeCG(loaders, scope, useOneCFA);
  }

  protected static JSCFABuilder makeCG(JavaScriptLoaderFactory loaders, AnalysisScope scope, boolean useOneCFA)
      throws IOException {
    try {
      IClassHierarchy cha = makeHierarchy(scope, loaders);
      com.ibm.wala.cast.test.Util.checkForFrontEndErrors(cha);
      Iterable<Entrypoint> roots = makeScriptRoots(cha);
      AnalysisOptions options = makeOptions(scope, cha, roots);
      AnalysisCache cache = makeCache();

      JSCFABuilder builder = new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, ZeroXInstanceKeys.ALLOCATIONS, useOneCFA);

      return builder;
    } catch (ClassHierarchyException e) {
      Assert.assertTrue("internal error building class hierarchy", false);
      return null;
    }
  }
}
