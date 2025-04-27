package com.ibm.wala.util.graph;

import org.assertj.core.api.Condition;
import org.jspecify.annotations.Nullable;

public class EdgeManagerConditions {

  public static <T> Condition<EdgeManager<T>> edge(@Nullable T src, @Nullable T dst) {
    return new Condition<>(actual -> actual.hasEdge(src, dst), "edge from %s to %s", src, dst);
  }
}
