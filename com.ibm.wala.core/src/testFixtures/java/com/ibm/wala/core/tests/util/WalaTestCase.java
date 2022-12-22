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

import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.heapTrace.HeapTracer;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
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

  @Before
  public void setUp() throws Exception {}

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

  /**
   * Utility function: each DetoxTestCase subclass can have a main() method that calls this, to
   * create a test suite consisting of just this test. Useful when investigating a single failing
   * test.
   */
  protected static void justThisTest(Class<?> testClass) {
    JUnitCore.runClasses(testClass);
  }

  protected static String getClasspathEntry(String elt) {
    final String normalizedElt = Paths.get(elt).normalize().toString();
    final String result =
        Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
            .map(candidate -> Paths.get(candidate).normalize())
            .filter(candidate -> candidate.toString().contains(normalizedElt))
            .map(
                found -> {
                  final String foundString = found.toString();
                  return found.toFile().isDirectory()
                      ? foundString + File.separatorChar
                      : foundString;
                })
            .collect(Collectors.joining(File.pathSeparator));
    assert !result.isEmpty() : "cannot find " + elt;
    return result;
  }
}
