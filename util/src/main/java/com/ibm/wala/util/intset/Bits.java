/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.intset;

/** utilities for manipulating values at the bit-level. */
public class Bits {

  // there's no reason to instantiate this class.
  private Bits() {}

  /** Return the lower 8 bits (as an int) of an int */
  public static int lower8(int value) {
    return (value & 0xff);
  }

  /** Return the lower 16 bits (as an int) of an int */
  public static int lower16(int value) {
    return (value & 0xffff);
  }

  /** Return the upper 16 bits (as an int) of an int */
  public static int upper16(int value) {
    return value >>> 16;
  }

  /** Return the upper 24 bits (as an int) of an int */
  public static int upper24(int value) {
    return value >>> 8;
  }

  /** Return the lower 32 bits (as an int) of a long */
  public static int lower32(long value) {
    return (int) value;
  }

  /** Return the upper 32 bits (as an int) of a long */
  public static int upper32(long value) {
    return (int) (value >>> 32);
  }

  /** Does an int literal val fit in bits bits? */
  public static boolean fits(int val, int bits) {
    val >>= bits - 1;
    return (val == 0 || val == -1);
  }

  /**
   * Return the number of ones in the binary representation of an integer.
   *
   * <p>This operation simply calls {@link Integer#bitCount}. It replaces Hank Warren's Hacker's
   * Delight SWAR sequence, which this method previously used: five shift/add stages, twenty
   * operations, nearly all of them in the loop-carried dependency chain. A paired JMH benchmark
   * (both arms in one run, 4 forks) measured the intrinsic 52% to 75% faster from 64 to 8192 bits,
   * and 64% to 70% at 2048 bits.
   *
   * <p><b>How much this is worth on a real analysis.</b> The only significant caller is {@link
   * BitVectorBase#populationCount()}, which invokes this once per 32-bit word. Instrumenting that
   * loop over {@code testHelloAllEntrypoints} shows ~300k invocations covering ~65 words each, so
   * this method is reached ~19.6M times. At that size the measured saving is ~0.22 ns/word, about 4
   * ms of a 7.7 s analysis, or roughly 0.05%. That is far below what an end-to-end benchmark can
   * resolve, and it is kept because it is a strict reduction in work with identical semantics, not
   * because it shows up in a profile or an end-to-end timing.
   *
   * <p><b>Do not size this from a profile.</b> {@link Integer#bitCount} is small enough to be
   * inlined, but the 41-byte loop that calls it is only inlined when the call site is hot and is
   * rejected as "too large" at lower tiers. Its cost is split between its own frames and its
   * callers', so neither a high nor a zero sample count is conclusive. Use a call count and a
   * per-call measurement instead.
   *
   * <p>Semantics are unchanged. The equivalence is covered exhaustively for all 65,536 16-bit
   * patterns in {@code com.ibm.wala.util.intset.BitsTest}, alongside random, edge and split-long
   * cases.
   */
  public static int populationCount(int value) {
    return Integer.bitCount(value);
  }
}
