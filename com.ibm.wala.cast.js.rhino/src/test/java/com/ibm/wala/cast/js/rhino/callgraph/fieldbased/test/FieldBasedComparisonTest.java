package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.test.ExtractingToPredictableFileNames;
import com.ibm.wala.cast.js.test.TestSimplePageCallGraphShape;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import org.junit.Test;

public class FieldBasedComparisonTest extends AbstractFieldBasedTest {

  private void test(String file, Object[][] assertions, BuilderType builderType)
      throws WalaException, Error, CancelException {
    try (ExtractingToPredictableFileNames predictable = new ExtractingToPredictableFileNames()) {
      runTest(file, assertions, builderType);
    }
  }

  @Test(expected = AssertionError.class)
  public void testSkeletonPessimistic() throws WalaException, Error, CancelException {
    test(
        "pages/skeleton.html",
        TestSimplePageCallGraphShape.assertionsForSkeleton,
        BuilderType.PESSIMISTIC);
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

  @Test(expected = AssertionError.class)
  public void testSkeleton2Pessimistic() throws WalaException, Error, CancelException {
    test(
        "pages/skeleton2.html",
        TestSimplePageCallGraphShape.assertionsForSkeleton2,
        BuilderType.PESSIMISTIC);
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
