package com.ibm.wala.util.graph;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.util.graph.impl.BasicOrderedMultiGraph;
import org.junit.jupiter.api.Test;

class BasicOrderedMultiGraphTest {

  @Test
  void unsetSuccessorSlotReturnsNull() {
    OrderedMultiGraph<String> graph = new BasicOrderedMultiGraph<>();
    graph.addNode("source");
    graph.addNode("target");
    graph.addEdge(2, "source", "target");

    assertThat(graph.getSuccessor("source", 1)).isNull();
  }
}
