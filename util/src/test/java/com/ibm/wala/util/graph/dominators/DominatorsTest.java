package com.ibm.wala.util.graph.dominators;

import static org.assertj.core.api.Assertions.assertThat;

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
    Integer root = Integer.valueOf(0);
    g.addNode(root);
    for (int i = 1; i < n; i++) {
      Integer v = Integer.valueOf(i);
      g.addNode(v);
      g.addEdge(root, v);
    }

    Dominators<Integer> d = Dominators.make(g, root);

    for (int i = 1; i < n; i++) {
      assertThat(d.getIdom(Integer.valueOf(i))).isEqualTo(root);
    }
  }
}
