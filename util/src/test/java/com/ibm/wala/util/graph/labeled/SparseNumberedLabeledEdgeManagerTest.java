package com.ibm.wala.util.graph.labeled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.ibm.wala.util.graph.impl.SlowNumberedNodeManager;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests default-label insertion and nullable node lookups. */
class SparseNumberedLabeledEdgeManagerTest {

  /** Creates the two graph nodes used by the edge-manager tests. */
  private static SlowNumberedNodeManager<String> makeNodeManager() {
    SlowNumberedNodeManager<String> nodes = new SlowNumberedNodeManager<>();
    nodes.addNode("src");
    nodes.addNode("dst");
    return nodes;
  }

  @Test
  void unlabeledInsertionUsesConfiguredDefault() {
    SparseNumberedLabeledEdgeManager<String, String> edges =
        new SparseNumberedLabeledEdgeManager<>(makeNodeManager(), "default");

    edges.addEdge("src", "dst");

    assertThat(edges.getEdgeLabels("src", "dst")).isEqualTo(Set.of("default"));
  }

  @Test
  void unlabeledInsertionRequiresADefaultLabel() {
    SparseNumberedLabeledEdgeManager<String, String> edges =
        new SparseNumberedLabeledEdgeManager<>(makeNodeManager());

    assertThatIllegalArgumentException().isThrownBy(() -> edges.addEdge("src", "dst"));

    assertThat(edges.hasEdge("src", "dst")).isFalse();
  }

  @Test
  void nullNodeQueriesReturnNoNeighbors() {
    SparseNumberedLabeledEdgeManager<String, String> edges =
        new SparseNumberedLabeledEdgeManager<>(makeNodeManager());
    edges.addEdge("src", "dst", "label");

    assertSoftly(softly -> {
      softly.assertThat(edges.getPredNodes(null)).as("getPredNodes(null)").isExhausted();
      softly.assertThat(edges.getSuccNodes(null)).as("getSuccNodes(null)").isExhausted();
      softly.assertThat(edges.hasEdge(null, "dst")).as("hasEdge(null, dst)").isFalse();
    });
  }
}
