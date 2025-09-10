package com.ibm.wala.util.graph;

import org.assertj.core.api.Condition;
import org.jspecify.annotations.Nullable;

public class NodeManagerConditions {

  public static <T> Condition<NodeManager<T>> node(@Nullable T n) {
    return new Condition<>(actual -> actual.containsNode(n), "node %s", n);
  }
}
