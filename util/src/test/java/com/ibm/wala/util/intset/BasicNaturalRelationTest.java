package com.ibm.wala.util.intset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Iterator;
import java.util.NoSuchElementException;
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

  private static void assertExhausted(Iterator<?> iterator) {
    assertSoftly(softly -> {
      softly.assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
      softly.assertThat(iterator).isExhausted();
    });
  }
}
