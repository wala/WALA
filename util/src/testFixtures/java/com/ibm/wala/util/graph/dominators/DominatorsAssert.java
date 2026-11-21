package com.ibm.wala.util.graph.dominators;

import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.GraphAssert;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ObjectAssert;

public class DominatorsAssert<T> extends AbstractObjectAssert<DominatorsAssert<T>, Dominators<T>> {

  public DominatorsAssert(Dominators<T> actual) {
    super(actual, DominatorsAssert.class);
  }

  @CheckReturnValue
  public static <T> DominatorsAssert<T> assertThat(Dominators<T> actual) {
    return new DominatorsAssert<>(actual);
  }

  public AbstractIterableAssert<?, ? extends Iterable<? extends T>, T, ObjectAssert<T>>
      dominatorsOf(T node) {
    isNotNull();
    return Assertions.assertThat(Iterator2Iterable.make(actual.dominators(node)));
  }

  public GraphAssert<T> dominatorTree() {
    isNotNull();
    return GraphAssert.assertThat(actual.dominatorTree());
  }

  @Override
  public DominatorsAssert<T> has(Condition<? super Dominators<T>> condition) {
    super.has(condition);
    return this;
  }
}
