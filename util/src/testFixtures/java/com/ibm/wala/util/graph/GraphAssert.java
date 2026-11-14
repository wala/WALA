package com.ibm.wala.util.graph;

import org.assertj.core.annotation.CheckReturnValue;

/** Custom assertions for {@link Graph} instances, for use in WALA's own tests. */
public class GraphAssert<N> extends AbstractGraphAssert<GraphAssert<N>, N, Graph<N>> {

  public GraphAssert(Graph<N> actual) {
    super(actual, GraphAssert.class);
  }

  @CheckReturnValue
  public static <N> GraphAssert<N> assertThat(Graph<N> actual) {
    return new GraphAssert<>(actual);
  }
}
