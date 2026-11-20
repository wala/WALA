package com.ibm.wala.util.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class SparseVectorTest {

  @Test
  void missingIndexReturnsNull() {
    IVector<String> vector = new SparseVector<>();

    assertThat(vector.get(7)).isNull();
  }

  @Test
  void iteratorReturnsStoredNonNullValuesInIndexOrder() {
    SparseVector<String> vector = new SparseVector<>();
    vector.set(7, "seven");
    vector.set(2, "two");

    assertThat(vector).containsExactly("two", "seven");
  }

  @Test
  void iteratorPreservesStoredNullValues() {
    SparseVector<@Nullable String> vector = new SparseVector<>(2, 1.5f);
    vector.set(7, "seven");
    vector.set(2, null);

    assertThat(vector).containsExactly(null, "seven");
  }

  @Test
  void clearRemovesStoredValues() {
    SparseVector<String> vector = new SparseVector<>();
    vector.set(7, "seven");

    vector.clear();

    assertSoftly(softly -> {
      softly.assertThat(vector.get(7)).isNull();
      softly.assertThat(vector).isEmpty();
      softly.assertThat(vector.size()).isZero();
    });
  }
}
