/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.util;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.ir.AnnotationTest;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.heapTrace.HeapTracer;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.warnings.Warnings;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.JUnitCore;

/** Simple extension to JUnit test case. */
public abstract class WalaTestCase {

  private static final boolean ANALYZE_LEAKS = false;

  public static boolean useShortProfile() {
    String profile = System.getProperty("com.ibm.wala.junit.profile", "long");
    if (profile.equals("short")) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean analyzingJar() {
    return "true".equals(System.getProperty("com.ibm.wala.junit.analyzingJar"));
  }
  /*
   * @see junit.framework.TestCase#setUp()
   */
  @Before
  public void setUp() throws Exception {}

  /*
   * @see junit.framework.TestCase#tearDown()
   */
  @After
  public void tearDown() throws Exception {
    Warnings.clear();
    if (ANALYZE_LEAKS) {
      HeapTracer.analyzeLeaks();
    }
  }

  protected IAnalysisCacheView makeAnalysisCache() {
    return makeAnalysisCache(SSAOptions.defaultOptions());
  }

  protected IAnalysisCacheView makeAnalysisCache(SSAOptions ssaOptions) {
    return new AnalysisCacheImpl(ssaOptions);
  }

  public static IClassHierarchy makeCHA() throws IOException, ClassHierarchyException {
    AnalysisScope scope =
        AnalysisScopeReader.readJavaScope(
            TestConstants.WALA_TESTDATA,
            (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS),
            AnnotationTest.class.getClassLoader());
    return ClassHierarchyFactory.make(scope);
  }

  /**
   * Utility function: each DetoxTestCase subclass can have a main() method that calls this, to
   * create a test suite consisting of just this test. Useful when investigating a single failing
   * test.
   */
  protected static void justThisTest(Class<?> testClass) {
    JUnitCore.runClasses(testClass);
  }

  protected static String getClasspathEntry(String elt) {
    StringBuilder result = null;
    for (String s : System.getProperty("java.class.path").split(File.pathSeparator)) {
      if (s.contains(elt)) {
        File e = new File(s);
        assert e.exists() : elt + " expected to exist";
        if (e.isDirectory() && !s.endsWith("/")) {
          s += '/';
        }
        if (result == null) {
          result = new StringBuilder(s);
        } else {
          result.append(File.pathSeparator).append(s);
        }
      }
    }
    assert result != null : "cannot find " + elt;
    return result.toString();
  }
}
