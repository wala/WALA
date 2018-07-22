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

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.perf.StopwatchGC;

/**
 * Utilities for call graph tests
 */
public class CallGraphTestUtil {

  private static final ClassLoader MY_CLASSLOADER = CallGraphTestUtil.class.getClassLoader();

  public static AnalysisOptions makeAnalysisOptions(AnalysisScope scope, Iterable<Entrypoint> entrypoints) {
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
    return options;
  }

  public static String REGRESSION_EXCLUSIONS = "Java60RegressionExclusions.txt";

  public static String REGRESSION_EXCLUSIONS_FOR_GUI = "Java60RegressionExclusionsForGUI.txt";

  /**
   * should we check the heap footprint before and after CG construction?
   */
  private static final boolean CHECK_FOOTPRINT = false;

  public static AnalysisScope makeJ2SEAnalysisScope(String scopeFile, String exclusionsFile) throws IOException {
    return makeJ2SEAnalysisScope(scopeFile, exclusionsFile, MY_CLASSLOADER);
  }
  
  public static AnalysisScope makeJ2SEAnalysisScope(String scopeFile, String exclusionsFile, ClassLoader myClassLoader) throws IOException {
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, (new FileProvider()).getFile(exclusionsFile), myClassLoader);
    return scope;
  }

  public static CallGraph buildRTA(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha, AnalysisScope scope)
      throws IllegalArgumentException, CancelException {
    StopwatchGC S = null;
    if (CHECK_FOOTPRINT) {
      S = new StopwatchGC("build RTA graph");
      S.start();
    }

    CallGraphBuilder<InstanceKey> builder = Util.makeRTABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    if (CHECK_FOOTPRINT) {
      S.stop();
      System.err.println(S.report());
    }
    return cg;
  }

  public static CallGraph buildZeroCFA(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha, AnalysisScope scope,
      boolean testPAtoString) throws IllegalArgumentException, CancelException {
    StopwatchGC S = null;
    if (CHECK_FOOTPRINT) {
      S = new StopwatchGC("build RTA graph");
      S.start();
    }

    SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);
    if (testPAtoString) {
      builder.getPointerAnalysis().toString();
    }

    if (CHECK_FOOTPRINT) {
      S.stop();
      System.err.println(S.report());
    }
    return cg;
  }

  public static CallGraph buildVanillaZeroOneCFA(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha,
      AnalysisScope scope) throws IllegalArgumentException, CancelException {
    StopwatchGC S = null;
    if (CHECK_FOOTPRINT) {
      S = new StopwatchGC("build RTA graph");
      S.start();
    }

    CallGraphBuilder<InstanceKey> builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    if (CHECK_FOOTPRINT) {
      S.stop();
      System.err.println(S.report());
    }
    return cg;
  }

  public static CallGraph buildZeroOneCFA(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha, AnalysisScope scope,
      boolean testPAtoString) throws IllegalArgumentException, CancelException {
    StopwatchGC S = null;
    if (CHECK_FOOTPRINT) {
      S = new StopwatchGC("build RTA graph");
      S.start();
    }

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(Language.JAVA, options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);
    if (testPAtoString) {
      builder.getPointerAnalysis().toString();
    }

    if (CHECK_FOOTPRINT) {
      S.stop();
      System.err.println(S.report());
    }
    return cg;
  }

  public static CallGraph buildZeroContainerCFA(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha,
      AnalysisScope scope) throws IllegalArgumentException, CancelException {
    StopwatchGC S = null;
    if (CHECK_FOOTPRINT) {
      S = new StopwatchGC("build RTA graph");
      S.start();
    }

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroContainerCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    if (CHECK_FOOTPRINT) {
      S.stop();
      System.err.println(S.report());
    }
    return cg;
  }

  public static CallGraph buildZeroOneContainerCFA(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha,
      AnalysisScope scope) throws IllegalArgumentException, CancelException {
    StopwatchGC S = null;
    if (CHECK_FOOTPRINT) {
      S = new StopwatchGC("build RTA graph");
      S.start();
    }

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    if (CHECK_FOOTPRINT) {
      S.stop();
      System.err.println(S.report());
    }
    return cg;
  }

}
