package com.ibm.wala.core.tests.ir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.util.intset.IntIterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class SSASwitchInstructionTest {

  @Test
  void emptyLabelsIteratorThrowsOnExhaustion() {
    assertExhausted(new SSASwitchInstruction(0, 1, 0, new int[0]).iterateLabels());
  }

  @Test
  void labelsIteratorThrowsAfterLastCase() {
    IntIterator iterator =
        new SSASwitchInstruction(0, 1, 0, new int[] {7, 10, 8, 20}).iterateLabels();

    assertThat(iterator.next()).isEqualTo(7);
    assertThat(iterator.next()).isEqualTo(8);
    assertExhausted(iterator);
  }

  private static void assertExhausted(IntIterator iterator) {
    assertSoftly(softly -> {
      softly.assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
      softly.assertThat(iterator.hasNext()).isFalse();
    });
  }
}
