package com.ibm.wala.util.intset;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

public final class PrimitiveIteratorAssertions {
  private PrimitiveIteratorAssertions() {}

  /** Checks that {@link IntIterator#next()} throws and leaves the iterator exhausted. */
  public static void assertExhausted(IntIterator iterator) {
    assertExhausted(iterator::next, iterator::hasNext);
  }

  /** Checks that {@link LongIterator#next()} throws and leaves the iterator exhausted. */
  public static void assertExhausted(LongIterator iterator) {
    assertExhausted(iterator::next, iterator::hasNext);
  }

  private static void assertExhausted(ThrowingCallable next, BooleanSupplier hasNext) {
    assertSoftly(softly -> {
      softly.assertThatThrownBy(next).isInstanceOf(NoSuchElementException.class);
      softly.assertThat(hasNext.getAsBoolean()).as("hasNext() after next()").isFalse();
    });
  }
}
