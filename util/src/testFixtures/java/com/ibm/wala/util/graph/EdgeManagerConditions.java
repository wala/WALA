package com.ibm.wala.util.graph;

import org.assertj.core.api.Condition;

public class EdgeManagerConditions {

  public static <T> Condition<EdgeManager<T>> edge(T source, T destination) {
    return new Condition<>(
        actual -> actual.hasEdge(source, destination),
        "edge from <%s> to <%s>",
        source,
        destination);
  }
}
