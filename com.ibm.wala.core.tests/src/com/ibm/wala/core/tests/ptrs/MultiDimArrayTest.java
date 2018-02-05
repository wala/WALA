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
package com.ibm.wala.core.tests.ptrs;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * 
 * Test for pointer analysis of multidimensional arrays
 * 
 * @author sfink
 */

public class MultiDimArrayTest extends WalaTestCase {


  public static void main(String[] args) {
    justThisTest(MultiDimArrayTest.class);
  }


  
  public MultiDimArrayTest() {
    
  }

  @Test public void testMultiDim() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util
        .makeMainEntrypoints(scope, cha, TestConstants.MULTI_DIM_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(),cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);
    PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();
    
    CGNode node = findDoNothingNode(cg);
    PointerKey pk = pa.getHeapModel().getPointerKeyForLocal(node, 1);
    OrdinalSet<InstanceKey> ptsTo = pa.getPointsToSet(pk);
    Assert.assertEquals(1, ptsTo.size());
  }
  
  private final static CGNode findDoNothingNode(CallGraph cg) {
    for (CGNode n : cg) {
      if (n.getMethod().getName().toString().equals("doNothing")) {
        return n;
      }
    }
    Assertions.UNREACHABLE("Unexpected: failed to find doNothing node");
    return null;
  }
}
