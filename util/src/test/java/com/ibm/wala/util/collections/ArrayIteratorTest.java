package com.ibm.wala.util.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ArrayIteratorTest {

  @Test
  void emptyArrayThrowsOnExhaustion() {
    assertExhausted(new ArrayIterator<>(new Integer[0]));
  }

  @Test
  void iteratorStopsAtInclusiveLastIndex() {
    Iterator<Integer> iterator = new ArrayIterator<>(new Integer[] {7, 8, 9}, 1, 1);

    assertThat(iterator.next()).isEqualTo(8);
    assertExhausted(iterator);
  }

  @Test
  void emptyRangeThrowsBeforeArrayEnds() {
    assertExhausted(new ArrayIterator<>(new Integer[] {7, 8}, 1, 0));
  }

  @Test
  void fullArrayThrowsAfterLastElement() {
    Iterator<Integer> iterator = new ArrayIterator<>(new Integer[] {7});

    assertThat(iterator.next()).isEqualTo(7);
    assertExhausted(iterator);
  }

  @Test
  void rangePastBackingArrayStillThrowsAtArrayBoundary() {
    Iterator<Integer> iterator = new ArrayIterator<>(new Integer[] {7}, 0, 1);

    assertThat(iterator.next()).isEqualTo(7);
    assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
  }

  private static void assertExhausted(Iterator<?> iterator) {
    assertSoftly(softly -> {
      softly.assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
      softly.assertThat(iterator).isExhausted();
    });
  }
}
