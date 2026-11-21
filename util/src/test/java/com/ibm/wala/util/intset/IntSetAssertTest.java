package com.ibm.wala.util.intset;

import static com.ibm.wala.util.intset.IntSetAssert.assertThat;
import static com.ibm.wala.util.intset.IntSetConditions.contains;
import static com.ibm.wala.util.intset.IntSetConditions.size;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IntSetAssertTest {

  @Test
  void testsIntSetAssertMethodsAndConditions() {
    MutableSparseIntSet set = MutableSparseIntSet.makeEmpty();
    assertThat(set).isEmpty();

    set.add(42);
    set.add(99);

    assertThat(set)
        .isNotEmpty()
        .hasSize(2)
        .contains(42)
        .contains(99)
        .doesNotContain(100)
        .has(size(2))
        .has(contains(42))
        .containsExactly(42, 99);
  }

  @Test
  void rejectsNullIntSet() {
    assertThatThrownBy(() -> assertThat(null).hasSize(0)).isInstanceOf(AssertionError.class);
  }
}
