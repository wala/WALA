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
   * Return the number of ones in the binary representation of an integer. Hank Warren's Hacker's
   * Delight algorithm
   */
  public static int populationCount(int value) {
    int result = ((value & 0xAAAAAAAA) >>> 1) + (value & 0x55555555);
    result = ((result & 0xCCCCCCCC) >>> 2) + (result & 0x33333333);
    result = ((result & 0xF0F0F0F0) >>> 4) + (result & 0x0F0F0F0F);
    result = ((result & 0xFF00FF00) >>> 8) + (result & 0x00FF00FF);
    result = ((result & 0xFFFF0000) >>> 16) + (result & 0x0000FFFF);
    return result;
  }
}
