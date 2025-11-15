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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.heapTrace.HeapTracer;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

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

  @BeforeEach
  public void setUp() throws Exception {}

  @AfterEach
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
    final var listener = new SummaryGeneratingListener();
    LauncherFactory.create()
        .execute(
            LauncherDiscoveryRequestBuilder.request().selectors(selectClass(testClass)).build(),
            listener);
    listener.getSummary().printFailuresTo(new PrintWriter(System.err));
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
    assertThat(result).as("cannot find %s", elt).isNotEmpty();
    return result;
  }
}
