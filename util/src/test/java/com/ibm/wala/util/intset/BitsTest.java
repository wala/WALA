package com.ibm.wala.util.intset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Pins the semantics of {@link Bits#populationCount(int)} and its callers' assumptions.
 *
 * <p>{@link Bits#populationCount} is now {@link Integer#bitCount} rather than a twenty-operation
 * SWAR sequence, so the contract is worth testing rather than assuming. Note the shape of the
 * exhaustive sweep: it places each of the 65,536 16-bit patterns in the low half and, separately,
 * in the high half of a 32-bit value. It does not combine the two halves; the random and split-long
 * cases are what cover values that mix them.
 *
 * <p>Deliberately scoped to {@link Bits#populationCount(int)}, the only method this file's change
 * touched. The other {@link Bits} helpers are covered elsewhere.
 */
public class BitsTest {

  @Test
  public void populationCountMatchesBitCountForEvery16BitPattern() {
    for (int pattern = 0; pattern <= 0xFFFF; pattern++) {
      int low = pattern;
      int high = pattern << 16;
      assertEquals(
          Integer.bitCount(low), Bits.populationCount(low), "low half, pattern " + pattern);
      assertEquals(
          Integer.bitCount(high), Bits.populationCount(high), "high half, pattern " + pattern);
    }
  }

  @Test
  public void populationCountMatchesBitCountOnRandomValues() {
    Random r = new Random(0x5EED);
    for (int i = 0; i < 1_000_000; i++) {
      int v = r.nextInt();
      assertEquals(Integer.bitCount(v), Bits.populationCount(v), "value " + v);
    }
  }

  @Test
  public void populationCountHandlesEdges() {
    assertEquals(0, Bits.populationCount(0));
    assertEquals(32, Bits.populationCount(-1));
    assertEquals(1, Bits.populationCount(1));
    assertEquals(1, Bits.populationCount(Integer.MIN_VALUE)); // MSB only
    assertEquals(31, Bits.populationCount(0x7FFFFFFF)); // all but MSB
  }

  @Test
  public void populationCountIsNonNegativeAndBounded() {
    Random r = new Random(0xB0B);
    for (int i = 0; i < 100_000; i++) {
      int c = Bits.populationCount(r.nextInt());
      assertTrue(c >= 0 && c <= 32, "out of range: " + c);
    }
  }

  @Test
  public void lowerAndUpper32SplitAgreeWithPopulationCount() {
    Random r = new Random(0xCAFE);
    for (int i = 0; i < 100_000; i++) {
      long v = r.nextLong();
      assertEquals(
          Bits.populationCount(Bits.lower32(v)) + Bits.populationCount(Bits.upper32(v)),
          Long.bitCount(v),
          "value " + v);
    }
  }
}
