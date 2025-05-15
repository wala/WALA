package com.ibm.wala.util.intset;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.util.CheckReturnValue;

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
}
