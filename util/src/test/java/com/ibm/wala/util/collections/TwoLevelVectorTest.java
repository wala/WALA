package com.ibm.wala.util.collections;

import static com.ibm.wala.util.collections.IteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TwoLevelVectorTest {

  @Test
  void emptyIteratorThrowsOnExhaustion() {
    assertExhausted(new TwoLevelVector<>().iterator());
  }

  @Test
  void iteratorCrossesMissingPagesAndThrowsAfterLastValue() {
    TwoLevelVector<String> vector = new TwoLevelVector<>();
    vector.set(1, "first");
    vector.set(8192, "last");
    Iterator<String> iterator = vector.iterator();

    assertThat(iterator.next()).isEqualTo("first");
    assertThat(iterator.next()).isEqualTo("last");
    assertExhausted(iterator);
  }

  @Test
  void storedNullIsAnElementBeforeExhaustion() {
    TwoLevelVector<@Nullable String> vector = new TwoLevelVector<>();
    vector.set(0, null);
    Iterator<@Nullable String> iterator = vector.iterator();

    assertThat(iterator).hasNext();
    assertThat(iterator.next()).isNull();
    assertExhausted(iterator);
  }
}
