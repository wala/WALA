package com.ibm.wala.util.graph;

import com.ibm.wala.util.collections.Iterator2Collection;
import java.util.Collection;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

/** Base class for assertions about graph nodes and edges. */
public abstract class AbstractGraphAssert<
        SELF extends AbstractGraphAssert<SELF, NODE, ACTUAL>, NODE, ACTUAL extends Graph<NODE>>
    extends AbstractEdgeManagerAssert<SELF, NODE, ACTUAL> {

  protected AbstractGraphAssert(ACTUAL actual, Class<?> selfType) {
    super(actual, selfType);
  }

  public AbstractCollectionAssert<?, Collection<? extends NODE>, NODE, ObjectAssert<NODE>> nodes() {
    isNotNull();
    return Assertions.assertThat(Iterator2Collection.toSet(actual.iterator())).as("graph nodes");
  }
}
