package com.ibm.wala.util.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests lookup behavior for both {@link AbstractMultiMap} implementations. */
class MultiMapTest {

  @Nested
  class HashSets extends LookupTests {
    @Override
    MultiMap<String, Integer> makeMap(boolean create) {
      return new HashSetMultiMap<>(create);
    }
  }

  @Nested
  class ArraySets extends LookupTests {
    @Override
    MultiMap<String, Integer> makeMap(boolean create) {
      return new ArraySetMultiMap<>(create);
    }
  }

  abstract static class LookupTests {

    /** Creates a map with the requested behavior for missing non-null lookup keys. */
    abstract MultiMap<String, Integer> makeMap(boolean create);

    @Test
    void nullLookupReturnsEmptyWithoutCreatingAKey() {
      MultiMap<String, Integer> map = makeMap(false);

      Set<Integer> values = map.get(null);

      assertSoftly(softly -> {
        softly.assertThat(values).isEmpty();
        softly.assertThat(map.keySet()).isEmpty();
      });
    }

    @Test
    void nullLookupDoesNotCreateAKeyWhenCreationIsEnabled() {
      MultiMap<String, Integer> map = makeMap(true);

      Set<Integer> values = map.get(null);

      assertSoftly(softly -> {
        softly.assertThat(values).isEmpty();
        softly.assertThat(map.keySet()).isEmpty();
      });
    }

    @Test
    void missingNonNullLookupReturnsEmptyWithoutCreatingAKey() {
      MultiMap<String, Integer> map = makeMap(false);

      Set<Integer> values = map.get("missing");

      assertSoftly(softly -> {
        softly.assertThat(values).isEmpty();
        softly.assertThat(map.keySet()).isEmpty();
      });
    }

    @Test
    void missingNonNullLookupCreatesALiveSetWhenCreationIsEnabled() {
      MultiMap<String, Integer> map = makeMap(true);

      Set<Integer> values = map.get("key");
      values.add(1);

      assertThat(map.get("key")).containsExactly(1);
    }

    @Test
    void existingKeyLookupReturnsStoredValues() {
      MultiMap<String, Integer> map = makeMap(false);
      map.put("key", 1);

      assertThat(map.get("key")).containsExactly(1);
    }
  }
}
