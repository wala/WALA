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

import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 *  
 */
public class BitVector extends BitVectorBase<BitVector> {

  private static final long serialVersionUID = 9087259335807761617L;

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
   * Return a new FixedSizeBitVector as the XOR of two others
   */
  public static BitVector xor(BitVector b1, BitVector b2) {
    BitVector b = new BitVector(b1);
    b.xor(b2);
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

  /**
   * Sets all bits.
   */
  public final void setAll() {
    for (int i = 0; i < bits.length; i++) {
      bits[i] = MASK;
    }
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
   * Return a new bit string as the AND of two others.
   */
  public static BitVector andNot(BitVector b1, BitVector b2) {
    BitVector b = new BitVector(b1);
    b.andNot(b2);
    return b;
  }

}
