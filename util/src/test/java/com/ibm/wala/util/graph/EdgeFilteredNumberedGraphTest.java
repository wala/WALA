package com.ibm.wala.util.graph;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EdgeFilteredNumberedGraph}.
 *
 * <p>The tests check that successor and predecessor queries both honor the ignored-edge relation in
 * the direction of each edge, and agree with {@link EdgeFilteredNumberedGraph#hasEdge}.
 */
public class EdgeFilteredNumberedGraphTest {

  /**
   * Makes the graph {@code a -> b}, {@code a -> c}.
   *
   * @return the graph
   */
  private static SlowSparseNumberedGraph<String> makeGraph() {
    SlowSparseNumberedGraph<String> g = SlowSparseNumberedGraph.make();
    g.addNode("a");
    g.addNode("b");
    g.addNode("c");
    g.addEdge("a", "b");
    g.addEdge("a", "c");
    return g;
  }

  /**
   * Makes a view of {@code g} that ignores the single edge {@code src -> dst}.
   *
   * @param g the graph
   * @param src the source of the ignored edge
   * @param dst the destination of the ignored edge
   * @return the filtered view
   */
  private static EdgeFilteredNumberedGraph<String> ignoring(
      NumberedGraph<String> g, String src, String dst) {
    BasicNaturalRelation ignore = new BasicNaturalRelation();
    ignore.add(g.getNumber(src), g.getNumber(dst));
    return new EdgeFilteredNumberedGraph<>(g, ignore);
  }

  /**
   * Tests that an ignored edge is absent from both its source's successors and its target's
   * predecessors.
   */
  @Test
  public void ignoredEdgeIsRemovedInBothDirections() {
    EdgeFilteredNumberedGraph<String> f = ignoring(makeGraph(), "a", "b");
    assertThat(f.hasEdge("a", "b")).isFalse();
    assertThat(Iterator2Collection.toSet(f.getSuccNodes("a"))).containsExactly("c");
    assertThat(f.getSuccNodeCount("a")).isEqualTo(1);
    assertThat(Iterator2Collection.toSet(f.getPredNodes("b"))).isEmpty();
  }

  /** Tests that ignoring {@code b -> a}, which is not an edge, leaves the edge {@code a -> b}. */
  @Test
  public void reverseOfIgnoredPairIsKept() {
    EdgeFilteredNumberedGraph<String> f = ignoring(makeGraph(), "b", "a");
    assertThat(f.hasEdge("a", "b")).isTrue();
    assertThat(Iterator2Collection.toSet(f.getSuccNodes("a"))).containsExactlyInAnyOrder("b", "c");
    assertThat(f.getSuccNodeCount("a")).isEqualTo(2);
    assertThat(Iterator2Collection.toSet(f.getPredNodes("b"))).containsExactly("a");
  }
}
