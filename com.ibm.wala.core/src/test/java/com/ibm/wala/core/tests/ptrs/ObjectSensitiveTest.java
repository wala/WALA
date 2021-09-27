/*
 * Copyright (c) 2002 - 2020 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.ptrs;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/** test case for nObjBuilder */
public class ObjectSensitiveTest {

  @Test
  public void testObjSensitive1()
      throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
    doPointsToSizeTest(1, TestConstants.OBJECT_SENSITIVE_TEST1, 1);
  }

  @Test
  public void testObjSensitive2()
      throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
    // If n is set to 2, the pts of the parameter will be inaccurate
    doPointsToSizeTest(2, TestConstants.OBJECT_SENSITIVE_TEST2, 2);

    doPointsToSizeTest(3, TestConstants.OBJECT_SENSITIVE_TEST2, 1);
  }

  public static void doPointsToSizeTest(int n, String mainClass, int expectedSize)
      throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
    Pair<CallGraph, PointerAnalysis<InstanceKey>> pair = initCallGraph(n, mainClass);
    CallGraph cg = pair.fst;
    PointerAnalysis<InstanceKey> pa = pair.snd;

    // find the doNothing call, and check the parameter's points-to set
    CGNode doNothing = findDoNothingCall(cg, mainClass);

    LocalPointerKey localPointerKey =
        new LocalPointerKey(doNothing, doNothing.getIR().getParameter(0));
    OrdinalSet<InstanceKey> pts = pa.getPointsToSet(localPointerKey);

    Assert.assertEquals(pts.size(), expectedSize);
  }

  private static Pair<CallGraph, PointerAnalysis<InstanceKey>> initCallGraph(
      int n, String mainClass)
      throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, mainClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeVanillaNObjBuilder(n, options, new AnalysisCacheImpl(), cha);

    CallGraph cg = builder.makeCallGraph(options, null);
    PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();
    return Pair.make(cg, pa);
  }

  private static CGNode findDoNothingCall(CallGraph cg, String mainClass) {
    TypeReference mainClassTr =
        TypeReference.findOrCreate(ClassLoaderReference.Application, mainClass);
    MethodReference mr =
        MethodReference.findOrCreate(mainClassTr, "doNothing", "(Ljava/lang/Object;)V");
    Set<CGNode> nodes = cg.getNodes(mr);
    Assert.assertEquals(1, nodes.size());

    Optional<CGNode> firstMatched = nodes.stream().findFirst();
    Assert.assertTrue(firstMatched.isPresent());
    return firstMatched.get();
  }
}
