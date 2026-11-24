package com.ibm.wala.util.intset;

import static com.ibm.wala.util.collections.IteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class BitSetTest {

  @Test
  void emptyIteratorThrowsOnExhaustion() {
    MutableMapping<String> mapping = new MutableMapping<>(new String[] {"value"});

    assertExhausted(new BitSet<>(mapping).iterator());
  }

  @Test
  void iteratorThrowsAfterLastMappedValue() {
    MutableMapping<String> mapping = new MutableMapping<>(new String[] {"first", "last"});
    BitSet<String> set = new BitSet<>(mapping);
    set.add("first");
    set.add("last");
    Iterator<String> iterator = set.iterator();

    assertThat(iterator.next()).isEqualTo("first");
    assertThat(iterator.next()).isEqualTo("last");
    assertExhausted(iterator);
  }
}
