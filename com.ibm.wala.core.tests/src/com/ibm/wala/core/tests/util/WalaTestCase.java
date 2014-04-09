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
package com.ibm.wala.core.tests.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.JUnitCore;

import com.ibm.wala.util.heapTrace.HeapTracer;
import com.ibm.wala.util.warnings.Warnings;

/**
 * Simple extension to JUnit test case.
 */
public abstract class WalaTestCase {

  final private static boolean ANALYZE_LEAKS = false;

  public static boolean useShortProfile() {
    String profile = System.getProperty("com.ibm.wala.junit.profile", "long");
    if (profile.equals("short")) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * @see junit.framework.TestCase#setUp()
   */
  @Before
  public void setUp() throws Exception {
  }

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

  /**
   * Utility function: each DetoxTestCase subclass can have a main() method that calls this, to create a test suite consisting of
   * just this test. Useful when investigating a single failing test.
   */
  protected static void justThisTest(Class<?> testClass) {
    JUnitCore.runClasses(testClass);
  }

  protected static void assertBound(String tag, double quantity, double bound) {
    String msg = tag + ", quantity: " + quantity + ", bound:" + bound;
    System.err.println(msg);
    Assert.assertTrue(msg, quantity <= bound);
  }

  protected static void assertBound(String tag, int quantity, int bound) {
    String msg = tag + ", quantity: " + quantity + ", bound:" + bound;
    System.err.println(msg);
    Assert.assertTrue(msg, quantity <= bound);
  }

}
