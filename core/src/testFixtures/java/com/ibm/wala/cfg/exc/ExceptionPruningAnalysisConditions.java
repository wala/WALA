package com.ibm.wala.cfg.exc;

import org.assertj.core.api.Condition;

/** Custom {@link Condition} instances for {@link ExceptionPruningAnalysis}. */
public final class ExceptionPruningAnalysisConditions {

  public static final Condition<ExceptionPruningAnalysis<?, ?>> exceptions =
      new Condition<>(ExceptionPruningAnalysis::hasExceptions, "has exceptions");

  private ExceptionPruningAnalysisConditions() {}
}
