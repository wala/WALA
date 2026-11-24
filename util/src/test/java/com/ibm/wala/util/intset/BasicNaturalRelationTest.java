package com.ibm.wala.util.intset;

import static com.ibm.wala.util.collections.IteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import org.junit.jupiter.api.Test;

class BasicNaturalRelationTest {

  @Test
  void emptyIteratorThrowsOnExhaustion() {
    assertExhausted(new BasicNaturalRelation().iterator());
  }

  @Test
  void smallStoreIteratorThrowsAfterLastPair() {
    BasicNaturalRelation relation = new BasicNaturalRelation();
    relation.add(2, 7);
    Iterator<IntPair> iterator = relation.iterator();

    assertThat(iterator.next()).isEqualTo(new IntPair(2, 7));
    assertExhausted(iterator);
  }

  @Test
  void delegateIteratorThrowsAfterLastPair() {
    BasicNaturalRelation relation = new BasicNaturalRelation();
    relation.add(2, 7);
    relation.add(2, 8);
    Iterator<IntPair> iterator = relation.iterator();

    assertThat(iterator.next()).isEqualTo(new IntPair(2, 7));
    assertThat(iterator.next()).isEqualTo(new IntPair(2, 8));
    assertExhausted(iterator);
  }
}
