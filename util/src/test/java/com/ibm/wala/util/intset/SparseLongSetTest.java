package com.ibm.wala.util.intset;

import static com.ibm.wala.util.intset.PrimitiveIteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SparseLongSetTest {

  @Test
  void emptyIteratorThrowsOnExhaustion() {
    assertExhausted(new SparseLongSet().longIterator());
  }

  @Test
  void iteratorThrowsAfterLastElement() {
    LongIterator iterator = new SparseLongSet(new long[] {7}).longIterator();

    assertThat(iterator.next()).isEqualTo(7);
    assertExhausted(iterator);
  }

  @Test
  void iteratorStopsBeforeUnusedCapacity() {
    MutableSparseLongSet set = MutableSparseLongSet.createMutableSparseLongSet(4);
    set.add(7);
    LongIterator iterator = set.longIterator();

    assertThat(iterator.next()).isEqualTo(7);
    assertExhausted(iterator);
  }
}
