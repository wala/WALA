package com.ibm.wala.util.graph;

import static com.ibm.wala.util.collections.IteratorAssertions.assertExhausted;
import static com.ibm.wala.util.graph.EdgeManagerAssert.assertThat;
import static com.ibm.wala.util.graph.EdgeManagerConditions.edge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.ibm.wala.util.graph.impl.BasicEdgeManager;
import com.ibm.wala.util.graph.impl.SelfLoopAddedEdgeManager;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class SelfLoopAddedEdgeManagerTest {
  private final EdgeManager<String> edges =
      new SelfLoopAddedEdgeManager<>(new BasicEdgeManager<>());

  @Test
  void nullSourceHasNoEdgeToANode() {
    assertThat(edges).doesNotHave(edge(null, "target"));
  }

  @Test
  void nodeHasNoEdgeToNullDestination() {
    assertThat(edges).doesNotHave(edge("source", null));
  }

  @Test
  void twoNullEndpointsDoNotFormASelfLoop() {
    assertThat(edges).doesNotHave(edge(null, null));
  }

  @Test
  void nullSourceCanBeRejectedByTheWrappedManager() {
    var graph = SlowSparseNumberedGraph.<String>make();
    graph.addNode("target");
    var wrappedEdges = new SelfLoopAddedEdgeManager<>(graph);

    assertThatIllegalArgumentException().isThrownBy(() -> wrappedEdges.hasEdge(null, "target"));
  }

  @Test
  void equalNodesHaveASyntheticSelfLoop() {
    var numericEdges = new SelfLoopAddedEdgeManager<>(new BasicEdgeManager<BigInteger>());

    assertThat(numericEdges).has(edge(new BigInteger("128"), new BigInteger("128")));
  }

  @Test
  void existingEdgeBetweenDistinctNodesIsReported() {
    edges.addEdge("source", "target");

    assertThat(edges).has(edge("source", "target"));
  }

  @Test
  void predecessorsIncludeSyntheticSelfLoop() {
    edges.addEdge("source", "target");

    assertThat(edges).predecessorsOf("target").containsExactlyInAnyOrder("target", "source");
  }

  @Test
  void successorsIncludeSyntheticSelfLoop() {
    edges.addEdge("source", "target");

    assertThat(edges).successorsOf("source").containsExactlyInAnyOrder("source", "target");
  }

  @Test
  void nextThrowsAfterTheOnlySuccessorIsReturned() {
    var successors = edges.getSuccNodes("node");

    assertThat(successors.next()).isEqualTo("node");
    assertExhausted(successors);
  }
}
