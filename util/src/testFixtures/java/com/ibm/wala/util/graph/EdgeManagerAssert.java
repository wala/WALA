package com.ibm.wala.util.graph;

import org.assertj.core.annotation.CheckReturnValue;

/** Custom assertions for {@link EdgeManager} instances, for use in WALA's own tests. */
public class EdgeManagerAssert<N>
    extends AbstractEdgeManagerAssert<EdgeManagerAssert<N>, N, EdgeManager<N>> {

  public EdgeManagerAssert(EdgeManager<N> actual) {
    super(actual, EdgeManagerAssert.class);
  }

  @CheckReturnValue
  public static <N> EdgeManagerAssert<N> assertThat(EdgeManager<N> actual) {
    return new EdgeManagerAssert<>(actual);
  }
}
