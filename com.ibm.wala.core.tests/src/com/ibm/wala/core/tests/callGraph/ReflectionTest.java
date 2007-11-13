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
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ecore.java.impl.JavaPackageImpl;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

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

  public void testReflect1() throws WalaException, IllegalArgumentException, CancelException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, "Java60RegressionExclusions.xml");
    ClassHierarchy cha = ClassHierarchy.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, TestConstants.REFLECT1_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    Warnings.clear();
    CallGraphTest.doCallGraphs(options, new AnalysisCache(),cha, scope, null, useShortProfile(), false);
    for (Iterator<Warning> it = Warnings.iterator(); it.hasNext(); ) {
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
