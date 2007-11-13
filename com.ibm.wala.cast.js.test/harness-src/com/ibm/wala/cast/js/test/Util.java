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

import junit.framework.Assert;

import com.ibm.wala.cast.js.ipa.callgraph.*;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.util.WebUtil;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.debug.Assertions;

public class Util extends com.ibm.wala.cast.js.ipa.callgraph.Util {

  public static PropagationCallGraphBuilder makeScriptCGBuilder(String dir, String name, boolean useOneCFA) throws IOException {
    JavaScriptLoaderFactory loaders = Util.makeLoaders();

    URL script = Util.class.getClassLoader().getResource(dir + File.separator + name);
    if (script == null) {
      script = Util.class.getClassLoader().getResource(dir + "/" + name);
    }
    Assertions._assert(script != null, "cannot find " + dir + " and " + name);

    AnalysisScope scope;
    if (script.openConnection() instanceof JarURLConnection) {
      scope = makeScope(new URL[] { script }, loaders);
    } else {
      scope = makeScope(new SourceFileModule[] { makeSourceModule(script, dir, name) }, loaders);
    }

    return makeCG(loaders, true, scope, useOneCFA);
  }

  public static PropagationCallGraphBuilder makeScriptCGBuilder(String dir, String name) throws IOException {
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

  public static CallGraph makeScriptCG(SourceFileModule[] scripts, boolean useOneCFA) throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder b = makeCGBuilder(scripts, useOneCFA);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    dumpCG(b, CG);
    return CG;
  }

  public static PropagationCallGraphBuilder makeHTMLCGBuilder(URL url) throws IOException {
    SourceFileModule script = WebUtil.extractScriptFromHTML(url);
    return makeCGBuilder(new SourceFileModule[] { script }, false);
  }

  public static CallGraph makeHTMLCG(URL url) throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder b = makeHTMLCGBuilder(url);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    dumpCG(b, CG);
    return CG;
  }

  public static PropagationCallGraphBuilder makeCGBuilder(SourceFileModule[] scripts, boolean useOneCFA) throws IOException {
    JavaScriptLoaderFactory loaders = makeLoaders();
    AnalysisScope scope = makeScope(scripts, loaders);
    return makeCG(loaders, true, scope, useOneCFA);
  }

  protected static PropagationCallGraphBuilder makeCG(JavaScriptLoaderFactory loaders, boolean keepIRs, AnalysisScope scope, boolean useOneCFA)
      throws IOException {
    try {
      IClassHierarchy cha = makeHierarchy(scope, loaders);
      Iterable<Entrypoint> roots = makeScriptRoots(cha);
      AnalysisOptions options = makeOptions(scope, keepIRs, cha, roots);
      AnalysisCache cache = makeCache(keepIRs);

      JSCFABuilder builder = new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, null, ZeroXInstanceKeys.ALLOCATIONS, useOneCFA);

      return builder;
    } catch (ClassHierarchyException e) {
      Assert.assertTrue("internal error building class hierarchy", false);
      return null;
    }
  }
}
