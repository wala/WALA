package com.ibm.wala.ipa.callgraph.propagation;

import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.IterableAssert;

/** Custom assertions for {@link PointerAnalysis} instances, for use in WALA's own tests. */
public class PointerAnalysisAssert<T extends InstanceKey>
    extends AbstractObjectAssert<PointerAnalysisAssert<T>, PointerAnalysis<T>> {

  public PointerAnalysisAssert(PointerAnalysis<T> actual) {
    super(actual, PointerAnalysisAssert.class);
  }

  @CheckReturnValue
  public static <T extends InstanceKey> PointerAnalysisAssert<T> assertThat(
      PointerAnalysis<T> actual) {
    return new PointerAnalysisAssert<>(actual);
  }

  public IterableAssert<T> pointsToSetOf(PointerKey pointerKey) {
    isNotNull();
    return Assertions.assertThat(actual.getPointsToSet(pointerKey))
        .as("points-to set of <%s>", pointerKey);
  }
}
