package com.ibm.wala.util.graph.dominators;

import static com.ibm.wala.util.graph.dominators.DominatorsAssert.assertThat;
import static com.ibm.wala.util.graph.dominators.DominatorsConditions.dominatedBy;
import static com.ibm.wala.util.graph.dominators.DominatorsConditions.immediateDominator;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.not;

import com.ibm.wala.util.graph.GraphAssert;
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
}
