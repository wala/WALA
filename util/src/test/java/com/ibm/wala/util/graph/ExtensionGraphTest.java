package com.ibm.wala.util.graph;

import static com.ibm.wala.util.graph.EdgeManagerConditions.edge;
import static com.ibm.wala.util.graph.GraphAssert.assertThat;
import static com.ibm.wala.util.intset.IntSetAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.ibm.wala.util.graph.impl.ExtensionGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class ExtensionGraphTest {
  private final SlowSparseNumberedGraph<String> original =
      new SlowSparseNumberedGraph<>(0) {
        @Override
        public boolean containsNode(@Nullable String node) {
          return node != null && super.containsNode(node);
        }

        @Override
        public boolean hasEdge(@Nullable String source, @Nullable String target) {
          return source != null && target != null && super.hasEdge(source, target);
        }
      };

  private final ExtensionGraph<String> graph = new ExtensionGraph<>(original);

  @Test
  void getNumberRejectsNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> graph.getNumber(null));
  }

  @Test
  void getPredNodeNumbersRejectsNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> graph.getPredNodeNumbers(null));
  }

  @Test
  void getSuccNodeNumbersRejectsNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> graph.getSuccNodeNumbers(null));
  }

  @Test
  void numberedQueriesFindNodesAndEdgesInTheOriginalGraph() {
    original.addNode("source");
    original.addNode("target");
    original.addEdge("source", "target");

    assertAll(
        () -> assertThat(graph.getNumber("source")).as("number of source").isEqualTo(0),
        () -> assertThat(graph.getPredNodeNumbers("target")).containsExactly(0),
        () -> assertThat(graph.getSuccNodeNumbers("source")).containsExactly(1));
  }

  @Test
  void nullDestinationIsNotAnExtensionEdge() {
    original.addNode("other");
    original.addNode("target");
    graph.addNode("source");
    graph.addEdge("source", "target");

    assertAll(
        () -> assertThat(graph).doesNotHave(edge("source", null)),
        () -> assertThat(graph).has(edge("source", "target")),
        () -> assertThat(graph).doesNotHave(edge("source", "other")),
        () -> assertThat(graph).doesNotHave(edge("target", "source")));
  }
}
