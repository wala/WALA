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

import java.io.IOException;

import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.perf.StopwatchGC;

/**
 * Utilities for call graph tests
 * 
 * @author sfink
 */
public class CallGraphTestUtil {

  private static final ClassLoader MY_CLASSLOADER = CallGraphTestUtil.class.getClassLoader();


  public static AnalysisOptions makeAnalysisOptions(AnalysisScope scope, Iterable<Entrypoint> entrypoints) {
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
    return options;
  }

  public static String REGRESSION_EXCLUSIONS = "Java60RegressionExclusions.txt";

  public static AnalysisScope makeJ2SEAnalysisScope(String scopeFile, String exclusionsFile) throws IOException {
    AnalysisScope scope = AnalysisScopeReader.read(scopeFile, FileProvider.getFile(exclusionsFile), MY_CLASSLOADER);
    return scope;
  }

  public static CallGraph buildRTA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha, AnalysisScope scope)
      throws IllegalArgumentException, CancelException {
    StopwatchGC S = new StopwatchGC("build RTA graph");
    S.start();

    CallGraphBuilder builder = Util.makeRTABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha, AnalysisScope scope,
      boolean testPAtoString) throws IllegalArgumentException, CancelException {
    StopwatchGC S = new StopwatchGC("build ZeroCFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);
    if (testPAtoString) {
      builder.getPointerAnalysis().toString();
    }

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildVanillaZeroOneCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha,
      AnalysisScope scope) throws IllegalArgumentException, CancelException {
    StopwatchGC S = new StopwatchGC("build Vanilla 0-1-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroOneCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha, AnalysisScope scope,
      boolean testPAtoString) throws IllegalArgumentException, CancelException {
    StopwatchGC S = new StopwatchGC("build 0-1-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);
    if (testPAtoString) {
      builder.getPointerAnalysis().toString();
    }

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroContainerCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha,
      AnalysisScope scope) throws IllegalArgumentException, CancelException {
    StopwatchGC S = new StopwatchGC("build 0-1-Container-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroContainerCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroOneContainerCFA(AnalysisOptions options, AnalysisCache cache, ClassHierarchy cha,
      AnalysisScope scope) throws IllegalArgumentException, CancelException {
    StopwatchGC S = new StopwatchGC("build 0-1-Container-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

}
