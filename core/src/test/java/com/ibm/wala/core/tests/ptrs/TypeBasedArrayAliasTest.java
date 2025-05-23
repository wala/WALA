/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.ptrs;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.io.IOException;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

public class TypeBasedArrayAliasTest extends WalaTestCase {

  @Test
  public void testTypeBasedArrayAlias()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            cha, TestConstants.ARRAY_ALIAS_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    // RTA yields a TypeBasedPointerAnalysis
    CallGraphBuilder<InstanceKey> builder =
        Util.makeRTABuilder(options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);
    PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();

    CGNode node = findNode(cg, "testMayAlias1");
    PointerKey pk1 = pa.getHeapModel().getPointerKeyForLocal(node, 1);
    PointerKey pk2 = pa.getHeapModel().getPointerKeyForLocal(node, 2);
    assertThat(pa).has(mayAliased(pk1, pk2));

    node = findNode(cg, "testMayAlias2");
    pk1 = pa.getHeapModel().getPointerKeyForLocal(node, 1);
    pk2 = pa.getHeapModel().getPointerKeyForLocal(node, 2);
    assertThat(pa).has(mayAliased(pk1, pk2));

    node = findNode(cg, "testMayAlias3");
    pk1 = pa.getHeapModel().getPointerKeyForLocal(node, 1);
    pk2 = pa.getHeapModel().getPointerKeyForLocal(node, 2);
    assertThat(pa).has(mayAliased(pk1, pk2));
  }

  private static final CGNode findNode(CallGraph cg, String methodName) {
    for (CGNode n : cg) {
      if (n.getMethod().getName().toString().equals(methodName)) {
        return n;
      }
    }
    Assertions.UNREACHABLE("Unexpected: failed to find " + methodName + " node");
    return null;
  }

  private static Condition<PointerAnalysis<InstanceKey>> mayAliased(
      PointerKey pk1, PointerKey pk2) {
    return new Condition<>(
        actual -> {
          OrdinalSet<InstanceKey> ptsTo1 = actual.getPointsToSet(pk1);
          OrdinalSet<InstanceKey> ptsTo2 = actual.getPointsToSet(pk2);
          boolean foundIntersection = false;
          outer:
          for (InstanceKey i : ptsTo1) {
            for (InstanceKey j : ptsTo2) {
              if (i.equals(j)) {
                foundIntersection = true;
                break outer;
              }
            }
          }
          return foundIntersection;
        },
        "%s may alias %s",
        pk1,
        pk2);
  }
}
