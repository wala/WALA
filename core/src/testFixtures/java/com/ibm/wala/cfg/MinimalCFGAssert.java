package com.ibm.wala.cfg;

import com.ibm.wala.util.graph.AbstractGraphAssert;
import java.util.Collection;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;

/** Custom assertions for {@link MinimalCFG} instances, for use in WALA's own tests. */
public class MinimalCFGAssert<T>
    extends AbstractGraphAssert<MinimalCFGAssert<T>, T, MinimalCFG<T>> {

  public MinimalCFGAssert(MinimalCFG<T> actual) {
    super(actual, MinimalCFGAssert.class);
  }

  @CheckReturnValue
  public static <T> MinimalCFGAssert<T> assertThat(MinimalCFG<T> actual) {
    return new MinimalCFGAssert<>(actual);
  }

  public ObjectAssert<T> entry() {
    isNotNull();
    return Assertions.assertThat(actual.entry()).as("CFG entry");
  }

  public ObjectAssert<T> exit() {
    isNotNull();
    return Assertions.assertThat(actual.exit()).as("CFG exit");
  }

  public ObjectAssert<T> returnNode() {
    isNotNull();
    return normalPredecessorsOf(actual.exit()).singleElement().as("CFG return");
  }

  public AbstractCollectionAssert<?, Collection<? extends T>, T, ObjectAssert<T>>
      normalSuccessorsOf(T block) {
    isNotNull();
    return Assertions.assertThat(actual.getNormalSuccessors(block))
        .as("normal successors of <%s>", block);
  }

  public ListAssert<T> exceptionalSuccessorsOf(T block) {
    isNotNull();
    return Assertions.assertThat(actual.getExceptionalSuccessors(block))
        .as("exceptional successors of <%s>", block);
  }

  public AbstractCollectionAssert<?, Collection<? extends T>, T, ObjectAssert<T>>
      normalPredecessorsOf(T block) {
    isNotNull();
    return Assertions.assertThat(actual.getNormalPredecessors(block))
        .as("normal predecessors of <%s>", block);
  }
}
