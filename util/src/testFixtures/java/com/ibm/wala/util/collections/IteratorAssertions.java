package com.ibm.wala.util.collections;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class IteratorAssertions {
  private IteratorAssertions() {}

  /** Checks that {@link Iterator#next()} throws and leaves the iterator exhausted. */
  public static void assertExhausted(Iterator<?> iterator) {
    assertSoftly(softly -> {
      softly.assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
      softly.assertThat(iterator).isExhausted();
    });
  }
}
