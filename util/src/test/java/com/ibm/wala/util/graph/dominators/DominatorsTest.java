package com.ibm.wala.util.graph.dominators;

import static com.ibm.wala.util.graph.dominators.DominatorsAssert.assertThat;
import static com.ibm.wala.util.graph.dominators.DominatorsConditions.dominatedBy;
import static com.ibm.wala.util.graph.dominators.DominatorsConditions.immediateDominator;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.not;

import com.ibm.wala.util.graph.GraphAssert;
import com.ibm.wala.util.graph.impl.BasicGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class DominatorsTest {

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  public void wideStarGraphRunsInLinearTime() {
    int n = 20_000;
    SlowSparseNumberedGraph<Integer> g = SlowSparseNumberedGraph.make();
    g.addNode(0);
    for (int i = 1; i < n; i++) {
      g.addNode(i);
      g.addEdge(0, i);
    }

    Dominators<Integer> d = Dominators.make(g, 0);

    for (int i = 1; i < n; i++) {
      assertThat(d).has(immediateDominator(i, 0));
    }
  }

  @Test
  public void testDominatorsAssertAndConditions() {
    SlowSparseNumberedGraph<Integer> g = SlowSparseNumberedGraph.make();
    g.addNode(0);
    g.addNode(1);
    g.addNode(2);
    g.addEdge(0, 1);
    g.addEdge(1, 2);

    Dominators<Integer> d = Dominators.make(g, 0);

    assertThat(d)
        .has(
            allOf(
                dominatedBy(2, 1),
                dominatedBy(2, 0),
                not(dominatedBy(0, 2)),
                immediateDominator(2, 1),
                immediateDominator(1, 0)));

    assertThat(d).dominatorsOf(2).containsExactly(2, 1, 0);
  }

  @Test
  public void unreachableNodesAreIsolatedInTheDominatorTree() {
    SlowSparseNumberedGraph<Integer> g = SlowSparseNumberedGraph.make();
    g.addNode(0);
    g.addNode(1);
    g.addNode(2);
    g.addEdge(0, 1);

    GraphAssert<Integer> tree = assertThat(Dominators.make(g, 0)).dominatorTree();

    tree.predecessorsOf(2).isEmpty();
    tree.successorsOf(2).isEmpty();
    tree.predecessorsOf(0).isEmpty();
    tree.successorsOf(0).containsExactly(1);
    tree.predecessorsOf(1).containsExactly(0);
  }

  @Test
  public void nullHasNoImmediateDominatorInANumberedGraph() {
    SlowSparseNumberedGraph<Integer> g = SlowSparseNumberedGraph.make();
    g.addNode(0);

    assertThat(Dominators.make(g, 0)).has(immediateDominator(null, null));
  }

  @Test
  public void nullHasNoImmediateDominatorInAGenericGraph() {
    BasicGraph<Integer> g = new BasicGraph<>();
    g.addNode(0);

    assertThat(Dominators.make(g, 0)).has(immediateDominator(null, null));
  }

  @Test
  public void dominatorsAccountForPathsAcrossDfsBranches() {
    BasicGraph<Integer> g = new BasicGraph<>();
    for (int node = 0; node <= 10; node++) {
      g.addNode(node);
    }
    int[][] edges = {
      {0, 1}, {0, 2}, {0, 3}, {1, 4}, {5, 6}, {6, 7}, {6, 8},
      {6, 9}, {7, 4}, {7, 2}, {10, 3}, {4, 5}, {2, 9}, {8, 10}
    };
    for (int[] edge : edges) {
      g.addEdge(edge[0], edge[1]);
    }

    Dominators<Integer> d = Dominators.make(g, 0);

    assertThat(d).dominatorsOf(7).containsExactly(7, 6, 5, 4, 1, 0);
    assertThat(d).dominatorsOf(10).containsExactly(10, 8, 6, 5, 4, 1, 0);
    assertThat(d)
        .has(allOf(immediateDominator(2, 0), immediateDominator(3, 0), immediateDominator(9, 0)));
  }
}
