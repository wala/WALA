package com.ibm.wala.util.graph.dominators;

import java.util.Objects;
import org.assertj.core.api.Condition;

public final class DominatorsConditions {

  public static <T> Condition<Dominators<T>> dominatedBy(T node, T dominator) {
    return new Condition<>(
        actual -> actual.isDominatedBy(node, dominator), "<%s> dominated by <%s>", node, dominator);
  }

  public static <T> Condition<Dominators<T>> immediateDominator(T node, T idom) {
    return new Condition<>(
        actual -> Objects.equals(actual.getIdom(node), idom),
        "immediate dominator of <%s> is <%s>",
        node,
        idom);
  }

  private DominatorsConditions() {}
}
