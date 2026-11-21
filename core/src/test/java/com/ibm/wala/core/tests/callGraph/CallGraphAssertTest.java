package com.ibm.wala.core.tests.callGraph;

import static com.ibm.wala.ipa.callgraph.CallGraphAssert.assertThat;
import static com.ibm.wala.util.graph.EdgeManagerConditions.edge;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import org.junit.jupiter.api.Test;

class CallGraphAssertTest {

  @Test
  void checksCallGraphProperties() throws Exception {
    CallGraph callGraph =
        CHACallGraphTest.testCHA(
            TestConstants.WALA_TESTDATA,
            "Llambda/MethodRefs",
            CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    MethodReference targetMethod =
        MethodReference.findOrCreate(
            TypeReference.findOrCreate(ClassLoaderReference.Application, "Llambda/MethodRefs$C1"),
            Atom.findOrCreateUnicodeAtom("target"),
            Descriptor.findOrCreateUTF8("()V"));
    MethodReference missingMethod =
        MethodReference.findOrCreate(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Llambda/MethodRefs$Missing"),
            Atom.findOrCreateUnicodeAtom("target"),
            Descriptor.findOrCreateUTF8("()V"));

    CGNode target = callGraph.getNodes(targetMethod).iterator().next();
    CGNode predecessor = callGraph.getPredNodes(target).next();

    assertThat(callGraph)
        .has(edge(predecessor, target))
        .doesNotHave(edge(callGraph.getFakeRootNode(), target))
        .nodes(targetMethod)
        .containsExactly(target);
    assertThat(callGraph).nodes(missingMethod).isEmpty();
    assertThat(callGraph).possibleSites(predecessor, target).isNotEmpty();
    assertThat(callGraph).predecessorsOf(target).contains(predecessor);
    assertThat(callGraph).successorsOf(predecessor).contains(target);
  }

  @Test
  void rejectsNullCallGraphsBeforeAccessingThem() {
    assertThatThrownBy(() -> assertThat(null).nodes(null)).isInstanceOf(AssertionError.class);
  }
}
