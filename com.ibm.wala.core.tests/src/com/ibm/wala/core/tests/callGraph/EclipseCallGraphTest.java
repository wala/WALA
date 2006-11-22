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

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ecore.java.impl.JavaPackageImpl;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * Tests for Call Graph construction
 * 
 * @author sfink
 */

public class EclipseCallGraphTest extends WalaTestCase {

  static {
    JavaPackageImpl.init();
  }

  public static void main(String[] args) {
    justThisTest(EclipseCallGraphTest.class);
  }


  public void testOrgEclipseCoreResources() throws WalaException {
  // not ready yet .. SJF
  //    EclipseAnalysisScope scope = new EclipseAnalysisScope("org.eclipse.core.resources");
//    WarningSet warnings = new WarningSet();
//    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
//    Entrypoints entrypoints = new EclipseEntrypoints(scope, cha, false);
//    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
//
//    Trace.println("org.eclipse.core.resources set up warnings:\n");
//    Trace.print(warnings.toString());
//
//    CallGraphTest.doCallGraphs(options, cha, scope, null, true, true);
  }
}
