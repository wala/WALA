/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.intset;

import java.io.Serializable;

import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 *  
 */
public class BitVector implements Cloneable, Serializable {

  private static final long serialVersionUID = 9087259335807761617L;
  private final static int LOG_BITS_PER_UNIT = 5;
  private final static int BITS_PER_UNIT = 32;
  private final static int MASK = 0xffffffff;
  private final static int LOW_MASK = 0x1f;
  int bits[];
  
  private final static boolean DEBUG = false;

  /**
   * Convert bitIndex to a subscript into the bits[] array.
   */
  public static int subscript(int bitIndex) {
    return bitIndex >> LOG_BITS_PER_UNIT;
  }

  public BitVector() {
    this(1);
  }

  /**
   * Creates an empty string with the specified size.
   * 
   * @param nbits
   *          the size of the string
   */
  public BitVector(int nbits) {
    bits = new int[subscript(nbits) + 1];
  }

  /**
   * Expand this bit vector to size newCapacity.
   */
  void expand(int newCapacity) {
    int[] oldbits = bits;
    bits = new int[subscript(newCapacity) + 1];
    for (int i = 0; i < oldbits.length; i++) {
      bits[i] = oldbits[i];
    }
  }

  /**
   * Creates a copy of a Bit String
   * 
   * @param s
   *          the string to copy
   */
  public BitVector(BitVector s) {
    bits = new int[s.bits.length];
    copyBits(s);
  }

  /**
   * Sets all bits.
   */
  public final void setAll() {
    for (int i = 0; i < bits.length; i++) {
      bits[i] = MASK;
    }
  }

  /**
   * Sets a bit.
   * 
   * @param bit
   *          the bit to be set
   */
  public final void set(int bit) {
    int shiftBits = bit & LOW_MASK;
    int subscript = subscript(bit);
    if (subscript >= bits.length) {
      expand(bit);
    }

    try {
      bits[subscript] |= (1 << shiftBits);
    } catch (RuntimeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Clears all bits.
   */
  public final void clearAll() {
    for (int i = 0; i < bits.length; i++) {
      bits[i] = 0;
    }
  }

  /**
   * Clears a bit.
   * 
   * @param bit
   *          the bit to be cleared
   */
  public final void clear(int bit) {
    int ss = subscript(bit);
    if (ss >= bits.length) {
      return;
    }
    int shiftBits = bit & LOW_MASK;
    bits[ss] &= ~(1 << shiftBits);
  }

  /**
   * Gets a bit.
   * 
   * @param bit
   *          the bit to be gotten
   */
  public final boolean get(int bit) {
    if (DEBUG) {
      Assertions._assert(bit >= 0);
    }
    int ss = subscript(bit);
    if (ss >= bits.length) {
      return false;
    }
    int shiftBits = bit & LOW_MASK;
    return ((bits[ss] & (1 << shiftBits)) != 0);
  }

  /**
   * Logically NOT this bit string
   */
  public final void not() {
    for (int i = 0; i < bits.length; i++) {
      bits[i] ^= MASK;
    }
  }

  /**
   * Return the NOT of a bit string
   */
  public static BitVector not(BitVector s) {
    BitVector b = new BitVector(s);
    b.not();
    return b;
  }

  /**
   * Logically ANDs this bit set with the specified set of bits.
   * 
   * @param set
   *          the bit set to be ANDed with
   */
  public final void and(BitVector set) {
    if (this == set) {
      return;
    }
    int n = Math.min(bits.length, set.bits.length);
    for (int i = n - 1; i >= 0;) {
      bits[i] &= set.bits[i];
      i--;
    }
    for (int i = n; i < bits.length; i++) {
      bits[i] = 0;
    }
  }

  /**
   * Return a new bit string as the AND of two others.
   */
  public static BitVector and(BitVector b1, BitVector b2) {
    BitVector b = new BitVector(b1);
    b.and(b2);
    return b;
  }

  /**
   * Logically ORs this bit set with the specified set of bits.
   * 
   * @param set
   *          the bit set to be ORed with
   */
  public final void or(BitVector set) {
    if (this == set) { // should help alias analysis
      return;
    }
    ensureCapacity(set);
    int n = Math.min(bits.length, set.bits.length);
    for (int i = n - 1; i >= 0;) {
      bits[i] |= set.bits[i];
      i--;
    }
  }

  /**
   * @param set
   */
  private void ensureCapacity(BitVector set) {
    if (set.bits.length > bits.length) {
      expand(BITS_PER_UNIT * set.bits.length - 1);
    }
  }

  /**
   * Logically ORs this bit set with the specified set of bits. This is
   * performance-critical, and so, a little ugly in an attempt to help out the
   * compiler.
   * 
   * @param set
   * @return the number of bits added to this.
   */
  public final int orWithDelta(BitVector set) {
    int delta = 0;

    ensureCapacity(set);
    int[] otherBits = set.bits;
    int n = Math.min(bits.length, otherBits.length);
    for (int i = n - 1; i >= 0;) {
      int v1 = bits[i];
      int v2 = otherBits[i];
      if (v1 != v2) {
        delta -= Bits.populationCount(v1);
        int v3 = v1 | v2;
        delta += Bits.populationCount(v3);
        bits[i] = v3;
      }
      i--;
    }
    return delta;
  }

  /**
   * Return a new FixedSizeBitVector as the OR of two others
   */
  public static BitVector or(BitVector b1, BitVector b2) {
    BitVector b = new BitVector(b1);
    b.or(b2);
    return b;
  }

  /**
   * Logically XORs this bit set with the specified set of bits.
   * 
   * @param set
   *          the bit set to be XORed with
   */
  public final void xor(BitVector set) {
    ensureCapacity(set);
    int n = Math.min(bits.length, set.bits.length);
    for (int i = n - 1; i >= 0;) {
      bits[i] ^= set.bits[i];
      i--;
    }
  }

  /**
   * Check if the intersection of the two sets is empty
   * 
   * @param other
   *          the set to check intersection with
   */
  public final boolean intersectionEmpty(BitVector other) {
    int n = Math.min(bits.length, other.bits.length);
    for (int i = n - 1; i >= 0;) {
      if ((bits[i] & other.bits[i]) != 0) {
        return false;
      }
      i--;
    }
    return true;
  }

  /**
   * Copies the values of the bits in the specified set into this set.
   * 
   * @param set
   *          the bit set to copy the bits from
   */
  public final void copyBits(BitVector set) {
    int setLength = set.bits.length;
    bits = new int[setLength];
    for (int i = setLength - 1; i >= 0;) {
      bits[i] = set.bits[i];
      i--;
    }

  }

  /**
   * Gets the hashcode.
   */
  public int hashCode() {
    int h = 1234;
    for (int i = bits.length - 1; i >= 0;) {
      h ^= bits[i] * (i + 1);
      i--;
    }
    return h;
  }

  /**
   * How many bits are set?
   */
  public final int populationCount() {
    int count = 0;
    for (int i = 0; i < bits.length; i++) {
      count += Bits.populationCount(bits[i]);
    }
    return count;
  }

  /**
   * Calculates and returns the set's size in bits. The maximum element in the
   * set is the size - 1st element.
   */
  public final int length() {
    return bits.length << LOG_BITS_PER_UNIT;
  }

  /**
   * Compares this object against the specified object.
   * 
   * @param B
   *          the object to compare with
   * @return true if the objects are the same; false otherwise.
   */
  public final boolean sameBits(BitVector B) {
    if (this == B) { // should help alias analysis
      return true;
    }
    int n = Math.min(bits.length, B.bits.length);
    if (bits.length > B.bits.length) {
      for (int i = n; i < bits.length; i++) {
        if (bits[i] != 0)
          return false;
      }
    } else if (B.bits.length > bits.length) {
      for (int i = n; i < B.bits.length; i++) {
        if (B.bits[i] != 0)
          return false;
      }
    }
    for (int i = n - 1; i >= 0;) {
      if (bits[i] != B.bits[i]) {
        return false;
      }
      i--;
    }
    return true;
  }

  /**
   * Compares this object against the specified object.
   * 
   * @param obj
   *          the object to compare with
   * @return true if the objects are the same; false otherwise.
   */
  public boolean equals(Object obj) {
    if ((obj != null) && (obj instanceof BitVector)) {
      if (this == obj) { // should help alias analysis
        return true;
      }
      BitVector set = (BitVector) obj;
      return sameBits(set);
    }
    return false;
  }

  public boolean isZero() {
    int setLength = bits.length;
    for (int i = setLength - 1; i >= 0;) {
      if (bits[i] != 0)
        return false;
      i--;
    }
    return true;
  }

  /**
   * Clones the FixedSizeBitVector.
   */
  public Object clone() {
    BitVector result = null;
    try {
      result = (BitVector) super.clone();
    } catch (CloneNotSupportedException e) {
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
    result.bits = new int[bits.length];
    System.arraycopy(bits, 0, result.bits, 0, result.bits.length);
    return result;
  }

  /**
   * Converts the FixedSizeBitVector to a String.
   */
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    boolean needSeparator = false;
    buffer.append('{');
    int limit = length();
    for (int i = 0; i < limit; i++) {
      if (get(i)) {
        if (needSeparator) {
          buffer.append(", ");
        } else {
          needSeparator = true;
        }
        buffer.append(i);
      }
    }
    buffer.append('}');
    return buffer.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#contains(int)
   */
  public boolean contains(int i) {
    return get(i);
  }

  private static final int[][] masks = new int[][] {
      { 0xFFFF0000 },
      { 0xFF000000, 0x0000FF00 },
      { 0xF0000000, 0x00F00000, 0x0000F000, 0x000000F0 },
      { 0xC0000000, 0x0C000000, 0x00C00000, 0x000C0000, 0x0000C000, 0x00000C00, 0x000000C0, 0x0000000C},
      { 0x80000000, 0x20000000, 0x08000000, 0x02000000, 0x00800000, 0x00200000, 0x00080000, 0x00020000, 0x00008000, 0x00002000, 0x00000800, 0x00000200, 0x00000080, 0x00000020, 0x00000008, 0x00000002 }
  };

  public int max() {
    int lastWord = bits.length - 1;
    int count = lastWord * BITS_PER_UNIT;

    int top = bits[lastWord];
    // Assertions._assert(top != 0);

    int j = 0;
    for (int i = 0; i < masks.length; i++) {
      if ((top & masks[i][j]) != 0) {
        j <<= 1;
      } else {
        j <<= 1;
        j++;
      }
    }

    return count + (31-j);
  }

  /**
   * @param start
   * @return min j >= start s.t get(j)
   */
  public int nextSetBit(int start) {
    int word = subscript(start);
    int bit = (1 << (start & LOW_MASK));
    while (word < bits.length) {
      if (bits[word] != 0) {
        do {
          if ((bits[word] & bit) != 0)
            return start;
          bit <<= 1;
          start++;
        } while (bit != 0);
      } else {
	start += (BITS_PER_UNIT - (start&LOW_MASK));
      }

      word++;
      bit = 1;
    }

    return -1;
  }

  /**
   * @param other
   * @return true iff this is a subset of other
   */
  public boolean isSubset(BitVector other) {
    if (this == other) { // should help alias analysis
      return true;
    }
    for (int i = 0; i < bits.length; i++) {
      if (i >= other.bits.length) {
        if (bits[i] != 0) {
          return false;
        }
      } else {
        if ((bits[i] & ~other.bits[i]) != 0) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * @param vector
   */
  public void andNot(BitVector vector) {
    int ai = 0;
    int bi = 0;
    for (ai = 0, bi = 0; ai < bits.length && bi < vector.bits.length; ai++, bi++) {
      bits[ai] = bits[ai] & (~vector.bits[bi]);
    }
  }
  
  /**
   * Return a new bit string as the AND of two others.
   */
  public static BitVector andNot(BitVector b1, BitVector b2) {
    BitVector b = new BitVector(b1);
    b.andNot(b2);
    return b;
  }

}
