package com.ibm.wala.util.math;

import static com.ibm.wala.util.math.LogsConditions.binaryLogUp;
import static com.ibm.wala.util.math.LogsConditions.powerOfTwo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@code int} and {@code long} overloads of {@code Logs.binaryLogUp}.
 *
 * <p>Both used to count with {@code while ((1 << k) < n) k++;}, which does not terminate for n
 * above 2^30 because a shift distance is taken modulo 32. These cases pin the result, including the
 * values that used to hang.
 *
 * <p>The oracle here doubles a running value rather than counting leading zeros, so it is a
 * genuinely different algorithm from the implementation. It carries {@code width} because a
 * doubling reference has the same overflow trap when written carelessly: accumulating in {@code
 * long} and testing {@code p < n} never terminates once p wraps to zero.
 */
@SuppressWarnings("removal")
class LogsTest {

  @Test
  void assertionsAreAvailableAsConditions() {
    assertThat(16).is(powerOfTwo).has(binaryLogUp(4));
  }

  /**
   * Smallest k with 2^k >= n, by repeated doubling. {@code width} is 32 for the int overload and 64
   * for the long one, and bounds k so the comparison cannot outlive the width.
   */
  private static int referenceLogUp(long n, int width) {
    if (n <= 1) {
      return 0;
    }
    int k = 0;
    long p = 1;
    while (k < width - 1 && p < n) {
      p <<= 1;
      k++;
    }
    return k;
  }

  private static int referenceLogUp(int n) {
    return referenceLogUp(n, Integer.SIZE);
  }

  private static int referenceLogUpLong(long n) {
    return referenceLogUp(n, Long.SIZE);
  }

  @Test
  void zeroAndOneNeedNoWidth() {
    assertThat(0).has(binaryLogUp(0));
    assertThat(1).has(binaryLogUp(0));
    assertThat(0L).has(binaryLogUp(0L));
    assertThat(1L).has(binaryLogUp(0L));
  }

  @Test
  void exactPowersOfTwo() {
    for (int k = 0; k < 31; k++) {
      int n = 1 << k;
      assertThat(n).as("2^%d", k).has(binaryLogUp(k));
      assertThat((long) n).as("2^%d as long", k).has(binaryLogUp((long) k));
    }
    for (int k = 0; k < 63; k++) {
      assertThat(1L << k).as("2^%d as long", k).has(binaryLogUp((long) k));
    }
  }

  @Test
  void justAboveAPowerOfTwo() {
    for (int k = 0; k < 31; k++) {
      int n = (1 << k) + 1;
      assertThat(n).as("2^%d + 1", k).has(binaryLogUp(k + 1));
      assertThat((long) n).has(binaryLogUp((long) k + 1));
    }
    for (int k = 0; k < 62; k++) {
      assertThat((1L << k) + 1).as("2^%d + 1 as long", k).has(binaryLogUp((long) k + 1));
    }
  }

  @Test
  void justBelowAPowerOfTwo() {
    // k starts at 2: 2^1 - 1 is 1, which needs no width at all, so it maps to 0 rather than 1.
    for (int k = 2; k < 31; k++) {
      int n = (1 << k) - 1;
      assertThat(n).as("2^%d - 1", k).has(binaryLogUp(k));
    }
    assertThat((1 << 1) - 1).has(binaryLogUp(0));
  }

  @Test
  void intValuesAboveTwoToThirtyNoLongerHang() {
    // Every one of these spun forever before the fix.
    assertThat((1 << 30) + 1).has(binaryLogUp(31));
    assertThat(Integer.MAX_VALUE).has(binaryLogUp(31));
    assertThat(-1).has(binaryLogUp(0));
    assertThat(Integer.MIN_VALUE).has(binaryLogUp(0));
  }

  @Test
  void longValuesAboveTwoToSixtyTwoNoLongerHang() {
    assertThat((1L << 62) + 1).has(binaryLogUp(63L));
    // 3e9 does not fit an int, so this reaches the long overload; 2^31 is below it, 2^32 above.
    assertThat(3_000_000_000L).has(binaryLogUp(32L));
    assertThat(Long.MAX_VALUE).has(binaryLogUp(63L));
    assertThat(-1L).has(binaryLogUp(0L));
    assertThat(Long.MIN_VALUE).has(binaryLogUp(0L));
  }

  @Test
  void intMatchesOracleForRandomValues() {
    Random r = new Random(0xB1A7);
    for (int trial = 0; trial < 50_000; trial++) {
      int n = r.nextInt();
      // no .as(...) here: AssertJ formats eagerly, and that cost dominated 50k iterations
      if (Logs.binaryLogUp(n) != referenceLogUp(n)) {
        fail("binaryLogUp(int) disagreed at n=" + n + " (trial " + trial + ")");
      }
    }
  }

  @Test
  void longMatchesOracleForRandomValues() {
    Random r = new Random(0x10C);
    for (int trial = 0; trial < 50_000; trial++) {
      long n = r.nextLong();
      if (Logs.binaryLogUp(n) != referenceLogUpLong(n)) {
        fail("binaryLogUp(long) disagreed at n=" + n + " (trial " + trial + ")");
      }
    }
  }

  @Test
  void exhaustivelySmallNonNegativeRange() {
    // Every input up to 131072, which covers all the low boundaries.
    for (int n = 0; n <= 131_072; n++) {
      if (Logs.binaryLogUp(n) != referenceLogUp(n)) {
        fail("binaryLogUp(int) disagreed at n=" + n);
      }
    }
  }

  @Test
  void boundariesJustAboveTwoToThirty() {
    for (int n = (1 << 30) - 3; n <= (1 << 30) + 3; n++) {
      if (Logs.binaryLogUp(n) != referenceLogUp(n)) {
        fail("binaryLogUp(int) disagreed at n=" + n);
      }
    }
  }

  @Test
  void isPowerOfTwoAndLog2StillAgree() {
    for (int k = 0; k < 31; k++) {
      int n = 1 << k;
      assertThat(n).is(powerOfTwo);
      assertThat(Logs.log2(n)).isEqualTo(k);
    }
    assertThat(0).isNot(powerOfTwo);
    assertThat(-1).isNot(powerOfTwo);
  }
}
