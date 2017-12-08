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

import org.junit.Test;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;

public class DynamicCallGraphTest extends DynamicCallGraphTestBase {

  protected final String testJarLocation;
  
  protected DynamicCallGraphTest(String testJarLocation) {
    this.testJarLocation = testJarLocation;
  }
    
  public DynamicCallGraphTest() {
    this(getClasspathEntry("com.ibm.wala.core.testdata"));
  }
  
  private static CallGraph staticCG(String mainClass, String exclusionsFile) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, exclusionsFile != null? exclusionsFile: CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    return CallGraphTestUtil.buildZeroOneCFA(options, new AnalysisCacheImpl(), cha, scope, false);
  }

  @Test
  public void testGraph() throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, IllegalArgumentException, ClassHierarchyException, CancelException, InterruptedException  {
    instrument(testJarLocation);
    run("dynamicCG.MainClass", null);
    CallGraph staticCG = staticCG("LdynamicCG/MainClass", null);
    checkEdges(staticCG);
  }

  @Test
  public void testCallbacks() throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, IllegalArgumentException, ClassHierarchyException, CancelException, InterruptedException  {
    instrument(testJarLocation);
    run("dynamicCG.CallbacksMainClass", null);
    CallGraph staticCG = staticCG("LdynamicCG/CallbacksMainClass", null);
    checkEdges(staticCG);
  }

  @Test
  public void testExclusions() throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, IllegalArgumentException, ClassHierarchyException, CancelException, InterruptedException  {
    instrument(testJarLocation);
    run("dynamicCG.MainClass", "ShrikeTestExclusions.txt");
    CallGraph staticCG = staticCG("LdynamicCG/MainClass", "ShrikeTestExclusions.txt");
    checkEdges(staticCG);
  }

  @Test
  public void testLambdas() throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, IllegalArgumentException, ClassHierarchyException, CancelException, InterruptedException  {
    instrument(testJarLocation);
    run("lambda.SortingExample", null);
    CallGraph staticCG = staticCG("Llambda/SortingExample", null);
    checkEdges(staticCG);
  }

}
