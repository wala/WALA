package com.ibm.wala.util.collections;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class HeapTest {

  @Test
  void growingHeapPreservesElementsInPriorityOrder() {
    Heap<Integer> heap =
        new Heap<>(1) {
          @Override
          protected boolean compareElements(Integer left, Integer right) {
            return left < right;
          }
        };
    for (int value : new int[] {5, 1, 4, 2, 3, 2, 0}) {
      heap.insert(value);
    }

    List<Integer> removed = Stream.generate(heap::take).limit(7).toList();

    assertSoftly(softly -> {
      softly.assertThat(removed).containsExactly(0, 1, 2, 2, 3, 4, 5);
      softly.assertThat(heap.isEmpty()).isTrue();
    });
  }
}
