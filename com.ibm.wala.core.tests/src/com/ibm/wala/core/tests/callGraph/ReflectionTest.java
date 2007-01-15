/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.callGraph;

import java.util.Iterator;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ecore.java.impl.JavaPackageImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * Tests for Call Graph construction
 * 
 * @author sfink
 */

public class ReflectionTest extends WalaTestCase {

  static {
    JavaPackageImpl.init();
  }

  public static void main(String[] args) {
    justThisTest(ReflectionTest.class);
  }

  public void testReflect1() throws WalaException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.REFLECT1_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    Trace.println("testReflect1 set up warnings:\n");
    Trace.print(warnings.toString());

    warnings = CallGraphTest.doCallGraphs(options, cha, scope, null, useShortProfile(), false);
    if (warnings.size() > 0) {
      System.err.println(warnings);
    }
    for (Iterator<Warning> it = warnings.iterator(); it.hasNext(); ) {
      Warning w = (Warning)it.next();
      if (w.toString().indexOf("com/ibm/jvm") > 0) {
        continue;
      }
      if (w.toString().indexOf("Integer") >= 0) {
        assertTrue(w.toString(), false);
      }
    }
   
  }
}
