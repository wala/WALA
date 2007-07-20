/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.callGraph;

import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.Stopwatch;
import com.ibm.wala.util.debug.Trace;

/**
 * 
 * Utilities for call graph tests
 * 
 * @author sfink
 */
public class CallGraphTestUtil {

  private static final ClassLoader MY_CLASSLOADER = CallGraphTestUtil.class.getClassLoader();

  // private static final String reflectionFile = Config.SPECJVM_REFLECTION;

  public static AnalysisOptions makeAnalysisOptions(AnalysisScope scope, Iterable<Entrypoint> entrypoints) {
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

    // InputStream rStream =
    // CallGraphTestUtil.class.getClassLoader().getResourceAsStream(reflectionFile);
    // ReflectionSpecification R = new XMLReflectionReader(rStream, scope);
    // options.setReflectionSpec(R);
    return options;
  }

  public static AnalysisScope makeJ2SEAnalysisScope(String scopeFile) {
    return new EMFScopeWrapper(scopeFile, "J2SEClassHierarchyExclusions.xml", MY_CLASSLOADER);
  }

  public static AnalysisScope makeJ2SEAnalysisScope(String scopeFile, String exclusionsFile) {
    return new EMFScopeWrapper(scopeFile, exclusionsFile, MY_CLASSLOADER);
  }

  public static CallGraph buildRTA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha, AnalysisScope scope) {
    Stopwatch S = new Stopwatch("build RTA graph");
    S.start();

    CallGraphBuilder builder = Util.makeRTABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha, AnalysisScope scope) {
    Stopwatch S = new Stopwatch("build ZeroCFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildVanillaZeroOneCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha,
      AnalysisScope scope) {
    Stopwatch S = new Stopwatch("build Vanila 0-1-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroOneCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha, AnalysisScope scope) {
    Stopwatch S = new Stopwatch("build 0-1-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroContainerCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha,
      AnalysisScope scope) {
    Stopwatch S = new Stopwatch("build 0-1-Container-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroContainerCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroOneContainerCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha,
      AnalysisScope scope) {
    Stopwatch S = new Stopwatch("build 0-1-Container-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildOneCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha, AnalysisScope scope) {
    Stopwatch S = new Stopwatch("build 1-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeOneCFABuilder(options, cache, cha, MY_CLASSLOADER, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }
}
