package com.ibm.wala.util.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Iterator2Collection}. */
class Iterator2CollectionTest {

  @SafeVarargs
  private static <T> Iterator<T> iterator(T... elements) {
    return Arrays.stream(elements).iterator();
  }

  private static <T> Iterator<T> emptyIterator() {
    return EmptyIterator.instance();
  }

  /** Unit tests for {@link Iterator2Collection#toList}. */
  @Nested
  class ToList {
    @Test
    void distinct() {
      assertThat(Iterator2Collection.toList(iterator("a", "b", "c")))
          .containsExactly("a", "b", "c");
    }

    @Test
    void duplicates() {
      assertThat(Iterator2Collection.toList(iterator("a", "b", "a", "b")))
          .containsExactly("a", "b", "a", "b");
    }

    @Test
    void empty() {
      assertThat(Iterator2Collection.toList(emptyIterator())).isEmpty();
    }

    @Test
    void singleElement() {
      assertThat(Iterator2Collection.toList(iterator("x"))).containsExactly("x");
    }

    @Test
    void nullThrows() {
      assertThatIllegalArgumentException().isThrownBy(() -> Iterator2Collection.toList(null));
    }

    @Test
    void returnsList() {
      assertThat(Iterator2Collection.toList(iterator("a"))).isInstanceOf(List.class);
    }
  }

  /** Unit tests for {@link Iterator2Collection#toSet}. */
  @Nested
  class ToSet {
    @Test
    void distinctAndOrdered() {
      assertThat(Iterator2Collection.toSet(iterator("a", "c", "b"))).containsExactly("a", "c", "b");
    }

    @Test
    void duplicates() {
      assertThat(Iterator2Collection.toSet(iterator("a", "b", "a", "b"))).containsExactly("a", "b");
    }

    @Test
    void empty() {
      assertThat(Iterator2Collection.toSet(emptyIterator())).isEmpty();
    }

    @Test
    void singleElement() {
      assertThat(Iterator2Collection.toSet(iterator("x"))).containsExactly("x");
    }

    @Test
    void nullThrows() {
      assertThatIllegalArgumentException().isThrownBy(() -> Iterator2Collection.toSet(null));
    }

    @Test
    void returnsSet() {
      assertThat(Iterator2Collection.toSet(iterator("a"))).isInstanceOf(Set.class);
    }
  }
}
