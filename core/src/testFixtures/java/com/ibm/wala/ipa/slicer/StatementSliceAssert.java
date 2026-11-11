package com.ibm.wala.ipa.slicer;

import java.util.Collection;
import java.util.function.IntSupplier;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractAssert;

/** Custom assertions for statement slices, for use in WALA's own tests. */
public class StatementSliceAssert
    extends AbstractAssert<StatementSliceAssert, Collection<Statement>> {

  private StatementSliceAssert(Collection<Statement> actual) {
    super(actual, StatementSliceAssert.class);
  }

  @CheckReturnValue
  public static StatementSliceAssert assertThatSlice(Collection<Statement> actual) {
    return new StatementSliceAssert(actual);
  }

  public StatementSliceAssert hasAllocationCount(int expectedCount) {
    return hasAllocationCount(expectedCount, false);
  }

  public StatementSliceAssert hasAllocationCount(int expectedCount, boolean applicationOnly) {
    return hasCount(
        "allocation",
        expectedCount,
        applicationOnly ? "application loader only" : "all loaders",
        () -> SlicerUtil.countAllocations(actual, applicationOnly));
  }

  public StatementSliceAssert hasThrowCount(int expectedCount) {
    return hasThrowCount(expectedCount, false);
  }

  public StatementSliceAssert hasThrowCount(int expectedCount, boolean applicationOnly) {
    return hasCount(
        "throw",
        expectedCount,
        applicationOnly ? "application loader only" : "all loaders",
        () -> SlicerUtil.countThrows(actual, applicationOnly));
  }

  public StatementSliceAssert hasArrayLoadCount(int expectedCount) {
    return hasCount(
        "array load", expectedCount, "all loaders", () -> SlicerUtil.countAloads(actual));
  }

  public StatementSliceAssert hasNormalCount(int expectedCount) {
    return hasCount("normal", expectedCount, "all loaders", () -> SlicerUtil.countNormals(actual));
  }

  public StatementSliceAssert hasApplicationNormalCount(int expectedCount) {
    return hasCount(
        "application normal",
        expectedCount,
        "application loader only",
        () -> SlicerUtil.countApplicationNormals(actual));
  }

  public StatementSliceAssert hasConditionalCount(int expectedCount) {
    return hasCount(
        "conditional", expectedCount, "all loaders", () -> SlicerUtil.countConditionals(actual));
  }

  public StatementSliceAssert hasInvokeCount(int expectedCount) {
    return hasCount("invoke", expectedCount, "all loaders", () -> SlicerUtil.countInvokes(actual));
  }

  public StatementSliceAssert hasPutfieldCount(int expectedCount) {
    return hasCount(
        "putfield", expectedCount, "all loaders", () -> SlicerUtil.countPutfields(actual));
  }

  public StatementSliceAssert hasReturnCount(int expectedCount) {
    return hasCount("return", expectedCount, "all loaders", () -> SlicerUtil.countReturns(actual));
  }

  public StatementSliceAssert hasGetfieldCount(int expectedCount) {
    return hasGetfieldCount(expectedCount, false);
  }

  public StatementSliceAssert hasGetfieldCount(int expectedCount, boolean applicationOnly) {
    return hasCount(
        "getfield",
        expectedCount,
        applicationOnly ? "application loader only" : "all loaders",
        () -> SlicerUtil.countGetfields(actual, applicationOnly));
  }

  public StatementSliceAssert hasPutstaticCount(int expectedCount) {
    return hasCount(
        "putstatic", expectedCount, "all loaders", () -> SlicerUtil.countPutstatics(actual));
  }

  public StatementSliceAssert hasGetstaticCount(int expectedCount) {
    return hasCount(
        "getstatic", expectedCount, "all loaders", () -> SlicerUtil.countGetstatics(actual));
  }

  private StatementSliceAssert hasCount(
      String category, int expectedCount, String filter, IntSupplier count) {
    isNotNull();
    int actualCount = count.getAsInt();
    if (actualCount != expectedCount) {
      failWithMessage(
          "\nExpecting slice to contain exactly <%d> %s statement(s) (%s), but found <%d>.\nSlice: %s",
          expectedCount, category, filter, actualCount, actual);
    }
    return myself;
  }
}
