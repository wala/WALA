package com.ibm.wala.util.graph;

import static com.ibm.wala.util.collections.IteratorAssertions.assertExhausted;
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
    assertThat(edges.hasEdge(null, "target")).as("edge from null to target").isFalse();
  }

  @Test
  void nodeHasNoEdgeToNullDestination() {
    assertThat(edges.hasEdge("source", null)).as("edge from source to null").isFalse();
  }

  @Test
  void twoNullEndpointsDoNotFormASelfLoop() {
    assertThat(edges.hasEdge(null, null)).as("edge from null to null").isFalse();
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

    assertThat(numericEdges.hasEdge(new BigInteger("128"), new BigInteger("128")))
        .as("edge between equal nodes")
        .isTrue();
  }

  @Test
  void existingEdgeBetweenDistinctNodesIsReported() {
    edges.addEdge("source", "target");

    assertThat(edges.hasEdge("source", "target")).as("edge from source to target").isTrue();
  }

  @Test
  void syntheticSelfLoopPrecedesExistingPredecessors() {
    edges.addEdge("source", "target");

    assertThat(edges.getPredNodes("target")).toIterable().containsExactly("target", "source");
  }

  @Test
  void syntheticSelfLoopPrecedesExistingSuccessors() {
    edges.addEdge("source", "target");

    assertThat(edges.getSuccNodes("source")).toIterable().containsExactly("source", "target");
  }

  @Test
  void nextThrowsAfterTheOnlySuccessorIsReturned() {
    var successors = edges.getSuccNodes("node");

    assertThat(successors.next()).isEqualTo("node");
    assertExhausted(successors);
  }
}
