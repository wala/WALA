package com.ibm.wala.core.tests.slicer;

import static com.ibm.wala.ipa.slicer.StatementSliceAssert.assertThatSlice;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.ipa.slicer.Statement;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatementSliceAssertTest {

  @Test
  void checksSliceCountsAndSupportsFluentAssertions() {
    Collection<Statement> slice = List.of();

    assertThatSlice(slice)
        .hasAllocationCount(0)
        .hasAllocationCount(0, true)
        .hasThrowCount(0)
        .hasThrowCount(0, true)
        .hasArrayLoadCount(0)
        .hasNormalCount(0)
        .hasApplicationNormalCount(0)
        .hasConditionalCount(0)
        .hasInvokeCount(0)
        .hasPutfieldCount(0)
        .hasReturnCount(0)
        .hasGetfieldCount(0)
        .hasGetfieldCount(0, true)
        .hasPutstaticCount(0)
        .hasGetstaticCount(0);
  }

  @Test
  void includesCountFilterAndSliceInFailures() {
    Collection<Statement> slice = List.of();

    assertThatThrownBy(() -> assertThatSlice(slice).hasAllocationCount(1, true))
        .hasMessageContaining("exactly <1> allocation statement(s)")
        .hasMessageContaining("application loader only")
        .hasMessageContaining("found <0>")
        .hasMessageContaining("Slice: []");
  }

  @Test
  void rejectsNullSlicesBeforeCounting() {
    assertThatThrownBy(() -> assertThatSlice(null).hasNormalCount(0))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("actual not to be null");
  }
}
