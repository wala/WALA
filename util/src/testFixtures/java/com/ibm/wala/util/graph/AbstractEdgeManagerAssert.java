package com.ibm.wala.util.graph;

import com.ibm.wala.util.collections.Iterator2Collection;
import java.util.Collection;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

/** Base class for assertions about directed edges managed by an {@link EdgeManager}. */
public abstract class AbstractEdgeManagerAssert<
        SELF extends AbstractEdgeManagerAssert<SELF, NODE, ACTUAL>,
        NODE,
        ACTUAL extends EdgeManager<NODE>>
    extends AbstractObjectAssert<SELF, ACTUAL> {

  protected AbstractEdgeManagerAssert(ACTUAL actual, Class<?> selfType) {
    super(actual, selfType);
  }

  public AbstractCollectionAssert<?, Collection<? extends NODE>, NODE, ObjectAssert<NODE>>
      predecessorsOf(NODE node) {
    isNotNull();
    return Assertions.assertThat(Iterator2Collection.toSet(actual.getPredNodes(node)))
        .as("predecessors of <%s>", node);
  }

  public AbstractCollectionAssert<?, Collection<? extends NODE>, NODE, ObjectAssert<NODE>>
      successorsOf(NODE node) {
    isNotNull();
    return Assertions.assertThat(Iterator2Collection.toSet(actual.getSuccNodes(node)))
        .as("successors of <%s>", node);
  }

  public AbstractIntegerAssert<?> predecessorCountOf(NODE node) {
    isNotNull();
    return Assertions.assertThat(actual.getPredNodeCount(node))
        .as("predecessor count of <%s>", node);
  }

  public AbstractIntegerAssert<?> successorCountOf(NODE node) {
    isNotNull();
    return Assertions.assertThat(actual.getSuccNodeCount(node)).as("successor count of <%s>", node);
  }
}
