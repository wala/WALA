package com.ibm.wala.core.tests.ptrs;

import static com.ibm.wala.ipa.callgraph.propagation.PointerAnalysisAssert.assertThat;
import static com.ibm.wala.ipa.callgraph.propagation.PointerAnalysisConditions.mayAlias;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
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
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import org.junit.jupiter.api.Test;

class PointerAnalysisAssertTest {

  @Test
  void testsPointsToAndAliasAssertions() throws Exception {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        Util.makeMainEntrypoints(cha, TestConstants.ARRAY_ALIAS_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeRTABuilder(options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);
    PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();

    CGNode node =
        cg.stream()
            .filter(n -> n.getMethod().getName().toString().equals("testMayAlias1"))
            .findFirst()
            .orElseThrow();
    PointerKey pk1 = pa.getHeapModel().getPointerKeyForLocal(node, 1);
    PointerKey pk2 = pa.getHeapModel().getPointerKeyForLocal(node, 2);

    assertThat(pa).has(mayAlias(pk1, pk2));
    assertThat(pa).pointsToSetOf(pk1).isNotEmpty();
  }

  @Test
  void rejectsNullPointerAnalysis() {
    assertThatThrownBy(() -> assertThat(null).pointsToSetOf(null))
        .isInstanceOf(AssertionError.class);
  }
}
