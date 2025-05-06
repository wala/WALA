package com.ibm.wala.util.intset;

import org.assertj.core.api.Condition;

public class IntSetConditions {

  public static Condition<IntSet> contains(int expectedElement) {
    return new Condition<>(
        actual -> actual.contains(expectedElement), "contains %d", expectedElement);
  }

  public static Condition<IntSet> sameValueAs(IntSet other) {
    return new Condition<>(actual -> actual.sameValue(other), "same value as %s", other);
  }

  public static Condition<IntSet> size(final int expectedSize) {
    return new Condition<>(actual -> actual.size() == expectedSize, "size %d", expectedSize);
  }

  public static Condition<IntSet> subsetOf(IntSet other) {
    return new Condition<>(actual -> actual.isSubset(other), "subset of %s", other);
  }
}
