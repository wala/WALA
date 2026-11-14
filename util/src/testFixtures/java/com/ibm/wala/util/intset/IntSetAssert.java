package com.ibm.wala.util.intset;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

public class IntSetAssert extends AbstractObjectAssert<IntSetAssert, IntSet> {

  public IntSetAssert(final IntSet actual) {
    super(actual, IntSetAssert.class);
  }

  @CheckReturnValue
  public static IntSetAssert assertThat(IntSet actual) {
    return new IntSetAssert(actual);
  }

  public AbstractCollectionAssert<?, Collection<? extends Integer>, Integer, ObjectAssert<Integer>>
      toCollection() {
    isNotNull();
    final Set<Integer> collection = new HashSet<>();
    actual.foreach(collection::add);
    return Assertions.assertThat(collection);
  }

  public IntSetAssert isEmpty() {
    isNotNull();
    if (!actual.isEmpty()) {
      failWithMessage("\nExpecting that actual `IntSet` is empty but is not.");
    }
    return this;
  }

  public IntSetAssert isNotEmpty() {
    isNotNull();
    if (actual.isEmpty()) {
      failWithMessage("\nExpecting that actual `IntSet` is non-empty but is empty.");
    }
    return this;
  }

  public IntSetAssert hasSize(int expectedSize) {
    isNotNull();
    if (actual.size() != expectedSize) {
      failWithMessage(
          "\nExpecting IntSet to have size <%d> but had <%d>.", expectedSize, actual.size());
    }
    return this;
  }

  public IntSetAssert contains(int expectedElement) {
    isNotNull();
    if (!actual.contains(expectedElement)) {
      failWithMessage(
          "\nExpecting IntSet <%s> to contain <%d> but it does not.", actual, expectedElement);
    }
    return this;
  }

  public IntSetAssert containsExactly(int... expectedElements) {
    isNotNull();
    toCollection()
        .containsExactlyInAnyOrderElementsOf(Arrays.stream(expectedElements).boxed().toList());
    return this;
  }

  public IntSetAssert doesNotContain(int expectedElement) {
    isNotNull();
    if (actual.contains(expectedElement)) {
      failWithMessage(
          "\nExpecting IntSet <%s> not to contain <%d> but it does.", actual, expectedElement);
    }
    return this;
  }
}
