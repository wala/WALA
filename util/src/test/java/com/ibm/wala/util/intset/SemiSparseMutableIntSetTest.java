package com.ibm.wala.util.intset;

import static com.ibm.wala.util.intset.PrimitiveIteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SemiSparseMutableIntSetTest {

  @Test
  void emptyIteratorThrowsOnExhaustion() {
    assertExhausted(new SemiSparseMutableIntSet().intIterator());
  }

  @Test
  void sparseIteratorThrowsAfterLastElement() {
    SemiSparseMutableIntSet set = new SemiSparseMutableIntSet();
    set.add(7);
    IntIterator iterator = set.intIterator();

    assertThat(iterator.next()).isEqualTo(7);
    assertExhausted(iterator);
  }

  @Test
  void denseIteratorThrowsOnRepeatedCallsAfterExhaustion() {
    SemiSparseMutableIntSet set = denseSet();
    IntIterator iterator = set.intIterator();

    assertThat(toList(iterator))
        .containsExactlyElementsOf(IntStream.range(0, 1000).boxed().toList());
    assertExhausted(iterator);
    assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void mixedIteratorThrowsAfterSparseAndDenseValues() {
    SemiSparseMutableIntSet set = denseSet();
    set.add(1000000);
    IntIterator iterator = set.intIterator();

    assertThat(toList(iterator))
        .containsExactlyInAnyOrderElementsOf(
            IntStream.concat(IntStream.range(0, 1000), IntStream.of(1000000)).boxed().toList());
    assertExhausted(iterator);
  }

  private static SemiSparseMutableIntSet denseSet() {
    SemiSparseMutableIntSet set = new SemiSparseMutableIntSet();
    for (int i = 0; i < 1000; i++) {
      set.add(i);
    }
    return set;
  }

  private static List<Integer> toList(IntIterator iterator) {
    List<Integer> result = new ArrayList<>();
    while (iterator.hasNext()) {
      result.add(iterator.next());
    }
    return result;
  }
}
