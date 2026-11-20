package com.ibm.wala.util.collections;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimpleVectorTest {

  @Test
  void getReturnsNullForUnsetSlot() {
    SimpleVector<String> vector = new SimpleVector<>();
    vector.set(4, "value");

    assertThat(vector.get(1)).isNull();
  }

  @Test
  void getReturnsNullBeyondStorage() {
    SimpleVector<String> vector = new SimpleVector<>();

    assertThat(vector.get(10)).isNull();
  }

  @Test
  void getReturnsStoredValueAfterGrowth() {
    SimpleVector<String> vector = new SimpleVector<>();
    vector.set(4, "value");

    assertThat(vector.get(4)).isEqualTo("value");
  }

  @Test
  void getReturnsNullAfterClearingSlot() {
    SimpleVector<String> vector = new SimpleVector<>();
    vector.set(4, "value");
    vector.set(4, null);

    assertThat(vector.get(4)).isNull();
  }
}
