package com.ibm.wala.util.intset;

import org.assertj.core.api.Condition;

public class LongSetConditions {

  public static Condition<LongSet> sameValueAs(LongSet other) {
    return new Condition<>(actual -> actual.sameValue(other), "same value as %s", other);
  }

  public static Condition<LongSet> subsetOf(LongSet other) {
    return new Condition<>(actual -> actual.isSubset(other), "subset of %s", other);
  }
}
