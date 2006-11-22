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
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.Stopwatch;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * Utilities for call graph tests
 * 
 * @author sfink
 */
public class CallGraphTestUtil {

  private static final ClassLoader MY_CLASSLOADER = CallGraphTestUtil.class.getClassLoader();

//  private static final String reflectionFile = Config.SPECJVM_REFLECTION;

  public static AnalysisOptions makeAnalysisOptions(AnalysisScope scope, Entrypoints entrypoints) {
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

//    InputStream rStream = CallGraphTestUtil.class.getClassLoader().getResourceAsStream(reflectionFile);
//    ReflectionSpecification R = new XMLReflectionReader(rStream, scope);
//    options.setReflectionSpec(R);
    return options;
  }


  public static AnalysisScope makeJ2SEAnalysisScope(String scopeFile) {
    return new EMFScopeWrapper(scopeFile, "J2SEClassHierarchyExclusions.xml", MY_CLASSLOADER);
  }
  
  public static AnalysisScope makeJ2SEAnalysisScope(String scopeFile, String exclusionsFile) {
    return new EMFScopeWrapper(scopeFile, exclusionsFile, MY_CLASSLOADER);
  }

  public static CallGraph buildRTA(AnalysisOptions options, ClassHierarchy cha, AnalysisScope scope, WarningSet warnings) {
    Stopwatch S = new Stopwatch("build RTA graph");
    S.start();

    CallGraphBuilder builder = Util.makeRTABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroCFA(AnalysisOptions options, ClassHierarchy cha, AnalysisScope scope, WarningSet warnings) {
    Stopwatch S = new Stopwatch("build ZeroCFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }
  
  public static CallGraph buildVanillaZeroOneCFA(AnalysisOptions options, ClassHierarchy cha, AnalysisScope scope, WarningSet warnings) {
    Stopwatch S = new Stopwatch("build Vanila 0-1-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroOneCFA(AnalysisOptions options, ClassHierarchy cha, AnalysisScope scope, WarningSet warnings) {
    Stopwatch S = new Stopwatch("build 0-1-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroContainerCFA(AnalysisOptions options, ClassHierarchy cha, AnalysisScope scope,
      WarningSet warnings) {
    Stopwatch S = new Stopwatch("build 0-1-Container-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroContainerCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildZeroOneContainerCFA(AnalysisOptions options, ClassHierarchy cha, AnalysisScope scope,
      WarningSet warnings) {
    Stopwatch S = new Stopwatch("build 0-1-Container-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options, cha, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

  public static CallGraph buildOneCFA(AnalysisOptions options, ClassHierarchy cha, AnalysisScope scope, WarningSet warnings) {
    Stopwatch S = new Stopwatch("build 1-CFA graph");
    S.start();

    CallGraphBuilder builder = Util.makeOneCFABuilder(options, cha, MY_CLASSLOADER, scope, warnings);
    CallGraph cg = builder.makeCallGraph(options);

    S.stop();
    Trace.println(S.report());
    return cg;
  }

}
