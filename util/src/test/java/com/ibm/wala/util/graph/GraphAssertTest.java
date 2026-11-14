package com.ibm.wala.util.graph;

import static com.ibm.wala.util.graph.EdgeManagerConditions.edge;
import static com.ibm.wala.util.graph.GraphAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import org.junit.jupiter.api.Test;

class GraphAssertTest {

  @Test
  void checksNodesAndEdgesAndSupportsFluentAssertions() {
    SlowSparseNumberedGraph<String> graph = SlowSparseNumberedGraph.make();
    graph.addNode("a");
    graph.addNode("b");
    graph.addNode("c");
    graph.addEdge("a", "b");
    graph.addEdge("a", "c");

    assertThat(graph)
        .has(edge("a", "b"))
        .has(edge("a", "c"))
        .doesNotHave(edge("b", "a"))
        .successorCountOf("a")
        .isEqualTo(2);

    assertThat(graph).predecessorCountOf("b").isEqualTo(1);
    assertThat(graph).predecessorsOf("b").containsExactly("a");
    assertThat(graph).successorsOf("a").containsExactlyInAnyOrder("b", "c");
    assertThat(graph).nodes().containsExactlyInAnyOrder("a", "b", "c");
  }

  @Test
  void identifiesMissingEdgeInFailures() {
    SlowSparseNumberedGraph<String> graph = SlowSparseNumberedGraph.make();
    graph.addNode("a");
    graph.addNode("b");

    assertThatThrownBy(() -> assertThat(graph).has(edge("a", "b")))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("edge from <a> to <b>");
  }

  @Test
  void rejectsNullGraphsBeforeAccessingThem() {
    assertThatThrownBy(() -> assertThat((Graph<String>) null).nodes())
        .isInstanceOf(AssertionError.class);
  }
}
