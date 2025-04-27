package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.test.ExtractingToPredictableFileNames;
import com.ibm.wala.cast.js.test.TestSimplePageCallGraphShape;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import org.junit.jupiter.api.Test;

public class FieldBasedComparisonTest extends AbstractFieldBasedTest {

  private void test(String file, Object[][] assertions, BuilderType builderType)
      throws WalaException, Error, CancelException {
    try (ExtractingToPredictableFileNames predictable = new ExtractingToPredictableFileNames()) {
      runTest(file, assertions, builderType);
    }
  }

  @Test
  public void testSkeletonPessimistic() {
    assertThatExceptionOfType(AssertionError.class)
        .isThrownBy(
            () ->
                test(
                    "pages/skeleton.html",
                    TestSimplePageCallGraphShape.assertionsForSkeleton,
                    BuilderType.PESSIMISTIC));
  }

  @Test
  public void testSkeletonOptimistic() throws WalaException, Error, CancelException {
    test(
        "pages/skeleton.html",
        TestSimplePageCallGraphShape.assertionsForSkeleton,
        BuilderType.OPTIMISTIC);
  }

  @Test
  public void testSkeletonWorklist() throws WalaException, Error, CancelException {
    test(
        "pages/skeleton.html",
        TestSimplePageCallGraphShape.assertionsForSkeleton,
        BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test
  public void testSkeleton2Pessimistic() {
    assertThatExceptionOfType(AssertionError.class)
        .isThrownBy(
            () ->
                test(
                    "pages/skeleton2.html",
                    TestSimplePageCallGraphShape.assertionsForSkeleton2,
                    BuilderType.PESSIMISTIC));
  }

  @Test
  public void testSkeleton2Optimistic() throws WalaException, Error, CancelException {
    test(
        "pages/skeleton2.html",
        TestSimplePageCallGraphShape.assertionsForSkeleton2,
        BuilderType.OPTIMISTIC);
  }

  @Test
  public void testSkeleton2Worklist() throws WalaException, Error, CancelException {
    test(
        "pages/skeleton2.html",
        TestSimplePageCallGraphShape.assertionsForSkeleton2,
        BuilderType.OPTIMISTIC_WORKLIST);
  }
}
