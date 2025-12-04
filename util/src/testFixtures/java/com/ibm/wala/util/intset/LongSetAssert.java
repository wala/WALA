package com.ibm.wala.util.intset;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

public class LongSetAssert extends AbstractObjectAssert<LongSetAssert, LongSet> {

  public LongSetAssert(final LongSet actual) {
    super(actual, LongSetAssert.class);
  }

  @CheckReturnValue
  public static LongSetAssert assertThat(LongSet actual) {
    return new LongSetAssert(actual);
  }

  public AbstractCollectionAssert<?, Collection<? extends Long>, Long, ObjectAssert<Long>>
      toCollection() {
    isNotNull();
    final Set<Long> collection = new HashSet<>();
    actual.foreach(collection::add);
    return Assertions.assertThat(collection);
  }

  public LongSetAssert isEmpty() {
    isNotNull();
    if (!actual.isEmpty()) {
      failWithMessage("\nExpecting that actual `LongSet` is empty but is not.");
    }
    return this;
  }
}
