package com.ibm.wala.util.intset;

import static com.ibm.wala.util.intset.PrimitiveIteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class SparseIntSetTest {

  @Test
  void emptyIteratorThrowsOnExhaustion() {
    assertExhausted(new SparseIntSet().intIterator());
  }

  @Test
  void iteratorThrowsAfterLastElement() {
    IntIterator iterator = SparseIntSet.singleton(7).intIterator();

    assertThat(iterator.next()).isEqualTo(7);
    assertExhausted(iterator);
  }

  @Test
  void iteratorStopsBeforeUnusedCapacity() {
    MutableSparseIntSet set = MutableSparseIntSet.makeEmpty();
    set.add(7);
    IntIterator iterator = set.intIterator();

    assertThat(iterator.next()).isEqualTo(7);
    assertExhausted(iterator);
  }

  @Test
  void ordinalSetIteratorPropagatesExhaustionException() {
    OrdinalSet<String> set =
        new OrdinalSet<>(SparseIntSet.singleton(0), new MutableMapping<>(new String[] {"value"}));
    Iterator<String> iterator = set.iterator();

    assertThat(iterator.next()).isEqualTo("value");
    assertSoftly(softly -> {
      softly.assertThat(iterator).isExhausted();
      softly.assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
    });
  }
}
