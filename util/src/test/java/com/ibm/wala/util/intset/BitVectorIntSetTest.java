package com.ibm.wala.util.intset;

import static com.ibm.wala.util.intset.PrimitiveIteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;

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
}
