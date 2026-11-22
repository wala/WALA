package com.ibm.wala.cfg.exc;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.MinimalCFGAssert;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractAssert;

public class ExceptionPruningAnalysisAssert<I, T extends IBasicBlock<I>>
    extends AbstractAssert<ExceptionPruningAnalysisAssert<I, T>, ExceptionPruningAnalysis<I, T>> {

  private ExceptionPruningAnalysisAssert(final ExceptionPruningAnalysis<I, T> actual) {
    super(actual, ExceptionPruningAnalysisAssert.class);
  }

  @CheckReturnValue
  public static <I, T extends IBasicBlock<I>> ExceptionPruningAnalysisAssert<I, T> assertThat(
      ExceptionPruningAnalysis<I, T> actual) {
    return new ExceptionPruningAnalysisAssert<>(actual);
  }

  public MinimalCFGAssert<T> cfg() {
    isNotNull();
    return MinimalCFGAssert.assertThat(actual.getCFG()).as("cfg of <%s>", descriptionText());
  }
}
