/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.core.tests.shrike;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;

public class DynamicCallGraphTest extends DynamicCallGraphTestBase {

  private static String testJarLocation = getClasspathEntry("com.ibm.wala.core.testdata");
  
  private static String testMain = "dynamicCG.MainClass";
  
  private CallGraph staticCG(String exclusionsFile) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, exclusionsFile != null? exclusionsFile: CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchy.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "LdynamicCG/MainClass");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    return CallGraphTestUtil.buildZeroOneCFA(options, new AnalysisCache(), cha, scope, false);
  }

  @Test
  public void testGraph() throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassHierarchyException, CancelException  {
    instrument(testJarLocation);
    run(testMain, null);
    CallGraph staticCG = staticCG(null);
    checkEdges(staticCG);
  }

  @Test
  public void testExclusions() throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassHierarchyException, CancelException  {
    instrument(testJarLocation);
    run(testMain, "ShrikeTestExclusions.txt");
    CallGraph staticCG = staticCG("ShrikeTestExclusions.txt");
    checkEdges(staticCG);
  }

}
