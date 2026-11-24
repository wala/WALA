package com.ibm.wala.util.intset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class BitVectorIntSetTest {

  @Test
  void emptyIteratorThrowsOnExhaustion() {
    assertExhausted(new BitVectorIntSet().intIterator());
  }

  @Test
  void iteratorThrowsAfterLastSetBit() {
    BitVectorIntSet set = new BitVectorIntSet();
    set.add(7);
    set.add(65);
    IntIterator iterator = set.intIterator();

    assertThat(iterator.next()).isEqualTo(7);
    assertThat(iterator.next()).isEqualTo(65);
    assertExhausted(iterator);
  }

  private static void assertExhausted(IntIterator iterator) {
    assertSoftly(softly -> {
      softly.assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
      softly.assertThat(iterator.hasNext()).isFalse();
    });
  }
}
