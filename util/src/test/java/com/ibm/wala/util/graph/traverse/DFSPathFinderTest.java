package com.ibm.wala.util.graph.traverse;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import org.junit.jupiter.api.Test;

class DFSPathFinderTest {

  @Test
  void findsPathAfterBacktrackingPastCycle() {
    SlowSparseNumberedGraph<String> graph = SlowSparseNumberedGraph.make();
    graph.addNode("root");
    graph.addNode("deadEnd");
    graph.addNode("target");
    graph.addEdge("root", "deadEnd");
    graph.addEdge("deadEnd", "root");
    graph.addEdge("root", "target");

    DFSPathFinder<String> finder = new DFSPathFinder<>(graph, "root", "target"::equals);

    assertThat(finder.find()).containsExactly("target", "root");
  }
}
