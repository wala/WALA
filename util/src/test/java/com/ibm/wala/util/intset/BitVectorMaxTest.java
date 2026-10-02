package com.ibm.wala.util.intset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BitVectorBase#max()}.
 *
 * <p>{@code max} used to locate the highest set bit by walking a five-level table of masks,
 * building the count of leading zeros one bit at a time. It now asks {@link
 * Integer#numberOfLeadingZeros} directly. Every case below is checked against {@link
 * #referenceMax}, an independent scan from the top bit down, so the test is an oracle rather than a
 * restatement of the implementation.
 */
class BitVectorMaxTest {

  /**
   * Highest set bit index, found by brute force. Independent of the implementation under test, and
   * typed on the abstract base so it also covers {@link OffsetBitVector}, whose get() already
   * reports false below its offset.
   */
  private static int referenceMax(BitVectorBase<?> v) {
    for (int i = v.length() - 1; i >= 0; i--) {
      if (v.get(i)) {
        return i;
      }
    }
    return -1;
  }

  private static BitVector withBits(int... bits) {
    BitVector v = new BitVector(64);
    for (int b : bits) {
      v.set(b);
    }
    return v;
  }

  @Test
  void emptyVectorHasNoMax() {
    assertThat(new BitVector(64).max()).isEqualTo(-1);
    assertThat(referenceMax(new BitVector(64))).isEqualTo(-1);
  }

  @Test
  void singleBitAtEveryPosition() {
    for (int b = 0; b < 64; b++) {
      BitVector v = withBits(b);
      assertThat(v.max()).as("single set bit at %d", b).isEqualTo(b);
    }
  }

  @Test
  void topBitOnly() {
    BitVector v = new BitVector(64);
    v.set(31);
    assertThat(v.max()).isEqualTo(31);
    // the top word of a wider vector, so the word-level scan is exercised too
    BitVector wide = new BitVector(256);
    wide.set(224);
    assertThat(wide.max()).isEqualTo(224);
    assertThat(wide.max()).isEqualTo(referenceMax(wide));
  }

  @Test
  void everySuffixMask() {
    // all bits from position k to 63 set: k must be the reported max
    for (int k = 0; k < 64; k++) {
      BitVector v = new BitVector(64);
      for (int i = k; i < 64; i++) {
        v.set(i);
      }
      assertThat(v.max()).as("bits %d..63", k).isEqualTo(63);
      assertThat(v.max()).isEqualTo(referenceMax(v));
    }
  }

  @Test
  void everyPrefixMask() {
    // bits 0..k set: k must be the reported max, exercising the zero-skip of lower words
    for (int k = 0; k < 64; k++) {
      BitVector v = new BitVector(64);
      for (int i = 0; i <= k; i++) {
        v.set(i);
      }
      assertThat(v.max()).as("bits 0..%d", k).isEqualTo(k);
      assertThat(v.max()).isEqualTo(referenceMax(v));
    }
  }

  @Test
  void maxAcrossWordBoundaries() {
    // one bit set in each of several words, including the highest, so the backward word scan and
    // the leading-zero step both matter
    for (int bit : new int[] {0, 31, 32, 33, 63, 64, 95, 127, 128, 200, 255}) {
      BitVector v = new BitVector(256);
      v.set(bit);
      assertThat(v.max()).as("single set bit at %d", bit).isEqualTo(bit);
      assertThat(v.max()).isEqualTo(referenceMax(v));
    }
  }

  @Test
  void onlyHighestWordPopulated() {
    BitVector v = new BitVector(512);
    v.set(300);
    v.set(400);
    assertThat(v.max()).isEqualTo(400);
    assertThat(v.max()).isEqualTo(referenceMax(v));
  }

  @Test
  void randomVectorsMatchOracle() {
    Random r = new Random(0x5EED);
    for (int trial = 0; trial < 3000; trial++) {
      int words = 1 + r.nextInt(8);
      BitVector v = new BitVector(words << 5);
      int density = r.nextInt(101);
      for (int b = 0; b < v.length(); b++) {
        if (r.nextInt(100) < density) {
          v.set(b);
        }
      }
      assertThat(v.max()).as("trial %d", trial).isEqualTo(referenceMax(v));
    }
  }

  @Test
  void agreesWithLeadingZerosOnEverySingleBitWord() {
    // The identity the implementation relies on is: for a non-zero word w, the index of its
    // highest set bit is 31 - numberOfLeadingZeros(w). Pin it at every bit position, across the
    // full word range, rather than only where a vector happens to be built.
    for (int b = 0; b < 32; b++) {
      int word = 1 << b;
      assertThat(31 - Integer.numberOfLeadingZeros(word)).isEqualTo(b);
    }
  }

  @Test
  void offsetVectorAgreesWithReference() {
    // OffsetBitVector reports max() in absolute coordinates, and get() returns false below the
    // offset, so the brute-force oracle applies unchanged.
    OffsetBitVector v = new OffsetBitVector(128, 64);
    v.set(150);
    v.set(180);
    assertThat(v.max()).isEqualTo(180);
    assertThat(v.max()).isEqualTo(referenceMax(v));

    // NOTE: an empty OffsetBitVector reports offset - 1, not -1, because OffsetBitVector.max()
    // adds the offset to super.max() unconditionally. That predates this change and is left alone
    // here; it is pinned separately rather than asserted as correct.
  }
}
