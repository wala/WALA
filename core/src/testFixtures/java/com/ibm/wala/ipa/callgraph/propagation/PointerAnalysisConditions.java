package com.ibm.wala.ipa.callgraph.propagation;

import org.assertj.core.api.Condition;

/** Custom {@link Condition} instances for {@link PointerAnalysis}. */
public final class PointerAnalysisConditions {

  public static <T extends InstanceKey> Condition<PointerAnalysis<T>> mayAlias(
      PointerKey pk1, PointerKey pk2) {
    return new Condition<>(
        actual -> actual.getPointsToSet(pk1).containsAny(actual.getPointsToSet(pk2)),
        "%s may alias %s",
        pk1,
        pk2);
  }

  private PointerAnalysisConditions() {}
}
