package com.ibm.wala.util.collections;

import static com.ibm.wala.util.collections.IteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComposedIteratorTest {

  @Test
  void emptyOuterIteratorThrowsOnExhaustion() {
    assertExhausted(compose(List.of()));
  }

  @Test
  void emptyInnerIteratorsThrowOnExhaustion() {
    assertExhausted(compose(List.of(List.of(), List.of())));
  }

  @Test
  void skipsEmptyInnerIteratorsAndThrowsAfterLastElement() {
    Iterator<Integer> iterator =
        compose(List.of(List.of(), List.of(7), List.of(), List.of(8), List.of()));

    assertThat(iterator.next()).isEqualTo(7);
    assertThat(iterator.next()).isEqualTo(8);
    assertExhausted(iterator);
  }

  private static Iterator<Integer> compose(List<List<Integer>> lists) {
    return new ComposedIterator<>(lists.iterator()) {
      @Override
      public Iterator<Integer> makeInner(List<Integer> list) {
        return list.iterator();
      }
    };
  }
}
