package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.test.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.cast.js.test.TestSimplePageCallGraphShape;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public class FieldBasedComparisonTest extends AbstractFieldBasedTest {

  private void test(String file, Object[][] assertions, BuilderType builderType) throws IOException, WalaException, Error, CancelException {
    boolean save = JSSourceExtractor.USE_TEMP_NAME;
    try {
      JSSourceExtractor.USE_TEMP_NAME = false;
      runTest(file, assertions, builderType);
    } finally {
      JSSourceExtractor.USE_TEMP_NAME = save;
    }
  }

  @Test(expected = AssertionError.class)
  public void testSkeletonPessimistic() throws IOException, WalaException, Error, CancelException {
    test("pages/skeleton.html", TestSimplePageCallGraphShape.assertionsForSkeleton, BuilderType.PESSIMISTIC);
  }

  @Test
  public void testSkeletonOptimistic() throws IOException, WalaException, Error, CancelException {
    test("pages/skeleton.html", TestSimplePageCallGraphShape.assertionsForSkeleton, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testSkeletonWorklist() throws IOException, WalaException, Error, CancelException {
    test("pages/skeleton.html", TestSimplePageCallGraphShape.assertionsForSkeleton, BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test(expected = AssertionError.class)
  public void testSkeleton2Pessimistic() throws IOException, WalaException, Error, CancelException {
    test("pages/skeleton2.html", TestSimplePageCallGraphShape.assertionsForSkeleton2, BuilderType.PESSIMISTIC);
  }

  @Test
  public void testSkeleton2Optimistic() throws IOException, WalaException, Error, CancelException {
    test("pages/skeleton2.html", TestSimplePageCallGraphShape.assertionsForSkeleton2, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testSkeleton2Worklist() throws IOException, WalaException, Error, CancelException {
    test("pages/skeleton2.html", TestSimplePageCallGraphShape.assertionsForSkeleton2, BuilderType.OPTIMISTIC_WORKLIST);
  }

}
