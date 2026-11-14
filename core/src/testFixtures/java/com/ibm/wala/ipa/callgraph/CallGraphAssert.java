package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.AbstractGraphAssert;
import java.util.Collection;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.IterableAssert;
import org.assertj.core.api.ObjectAssert;

/** Custom assertions for {@link CallGraph} instances, for use in WALA's own tests. */
public class CallGraphAssert extends AbstractGraphAssert<CallGraphAssert, CGNode, CallGraph> {

  public CallGraphAssert(final CallGraph actual) {
    super(actual, CallGraphAssert.class);
  }

  @CheckReturnValue
  public static CallGraphAssert assertThat(CallGraph actual) {
    return new CallGraphAssert(actual);
  }

  public AbstractCollectionAssert<?, Collection<? extends CGNode>, CGNode, ObjectAssert<CGNode>>
      nodes(MethodReference method) {
    isNotNull();
    return Assertions.assertThat(actual.getNodes(method));
  }

  public IterableAssert<CallSiteReference> possibleSites(CGNode source, CGNode target) {
    isNotNull();
    return Assertions.assertThat(Iterator2Iterable.make(actual.getPossibleSites(source, target)));
  }
}
