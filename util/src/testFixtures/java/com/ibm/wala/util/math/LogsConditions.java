package com.ibm.wala.util.math;

import org.assertj.core.api.Condition;

/** AssertJ conditions for values produced or accepted by {@link Logs}. */
public final class LogsConditions {

  public static final Condition<Integer> powerOfTwo =
      new Condition<>(Logs::isPowerOf2, "a power of two");

  @SuppressWarnings("removal")
  public static Condition<Integer> binaryLogUp(int expected) {
    return new Condition<>(
        value -> Logs.binaryLogUp(value) == expected, "having binary ceiling log %d", expected);
  }

  @SuppressWarnings("removal")
  public static Condition<Long> binaryLogUp(long expected) {
    return new Condition<>(
        value -> Logs.binaryLogUp(value) == expected, "having binary ceiling log %d", expected);
  }

  private LogsConditions() {}
}
