package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.util.intset.OrdinalSet;
import org.assertj.core.api.Condition;

/** Custom {@link Condition} instances for {@link PointerAnalysis}. */
public final class PointerAnalysisConditions {

  public static <T extends InstanceKey> Condition<PointerAnalysis<T>> mayAlias(
      PointerKey pk1, PointerKey pk2) {
    return new Condition<>(
        actual -> {
          OrdinalSet<T> ptsTo1 = actual.getPointsToSet(pk1);
          OrdinalSet<T> ptsTo2 = actual.getPointsToSet(pk2);
          for (T i : ptsTo1) {
            for (T j : ptsTo2) {
              if (i.equals(j)) {
                return true;
              }
            }
          }
          return false;
        },
        "%s may alias %s",
        pk1,
        pk2);
  }

  private PointerAnalysisConditions() {}
}
