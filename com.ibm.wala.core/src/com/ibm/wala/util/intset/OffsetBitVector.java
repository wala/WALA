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
 * @author Julian Dolby (dolby@us.ibm.com)
 *  
 */
public final class OffsetBitVector extends BitVectorBase<OffsetBitVector> {

  int offset;

  private int wordDiff(int offset1, int offset2) {
    return
      (offset1>offset2)?
      (offset1-offset2)>>LOG_BITS_PER_UNIT:  
      - ((offset2-offset1)>>LOG_BITS_PER_UNIT);
  }

  /**
   * Expand this bit vector to size newCapacity.
   */
  private void expand(int newOffset, int newCapacity) {
    int wordDiff = wordDiff(newOffset, offset);

    int[] oldbits = bits;
    bits = new int[subscript(newCapacity) + 1];
    for (int i = 0; i < oldbits.length; i++) {
      bits[i-wordDiff] = oldbits[i];
    }
    offset = newOffset;
  }

  /**
   * @param set
   */
  private void ensureCapacity(int newOffset, int newCapacity) {
    if (newOffset < offset || newCapacity>(bits.length<< LOG_BITS_PER_UNIT)) {
      expand(newOffset, newCapacity);
    }
  }

//  private void ensureCapacity(OffsetBitVector set) {
//    int newOffset = Math.min(offset, set.offset);
//    int newCapacity = 
//      Math.max(length(),set.length())-newOffset;
//    ensureCapacity(newOffset, newCapacity);
//  }

  public OffsetBitVector() {
    this(0, 1);
  }

  /**
   * Creates an empty string with the specified size.
   * 
   * @param nbits
   *          the size of the string
   */
  public OffsetBitVector(int offset, int nbits) {
    offset = (offset&~LOW_MASK);
    this.offset = offset;
    this.bits = new int[subscript(nbits) + 1];
  }

  /**
   * Creates a copy of a Bit String
   * 
   * @param s
   *          the string to copy
   */
  public OffsetBitVector(OffsetBitVector s) {
    offset = s.offset;
    bits = new int[s.bits.length];
    copyBits(s);
  }

  public String toString() {
    return super.toString() + "(offset:" + offset + ")";
  }

  public int getOffset() {
    return offset;
  }

  /**
   * Sets a bit.
   * 
   * @param bit
   *          the bit to be set
   */
  public final void set(int bit) {
    int shiftBits;
    int subscript;
    if (bit < offset) {
      int newOffset = bit&~LOW_MASK;
      expand(newOffset, length()-newOffset);
      shiftBits = bit & LOW_MASK;
      subscript = 0;
    } else {
      bit -= offset;    
      shiftBits = bit & LOW_MASK;
      subscript = subscript(bit);
      if (subscript >= bits.length) {
	expand(offset, bit);
      }
    }

    try {
      bits[subscript] |= (1 << shiftBits);
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Clears a bit.
   * 
   * @param bit
   *          the bit to be cleared
   */
  public final void clear(int bit) {
    if (bit < offset) {
      return;
    }
    bit -= offset;

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
    if (bit < offset) {
      return false;
    }
    bit -= offset;

    int ss = subscript(bit);
    if (ss >= bits.length) {
      return false;
    }
    int shiftBits = bit & LOW_MASK;
    return ((bits[ss] & (1 << shiftBits)) != 0);
  }

  /**
   * @param start
   * @return min j >= start s.t get(j)
   */
  public int nextSetBit(int start) {
    int nb = super.nextSetBit(Math.max(0, start-offset));
    return nb == -1? -1: offset + nb;
  }

  /**
   * Logically NOT this bit string
   */
  public final void not() {
    if (offset != 0) {
      expand(0, offset+length());
    }
    for (int i = 0; i < bits.length; i++) {
      bits[i] ^= MASK;
    }
  }

  public int max() {
    return super.max() + offset;
  }

  /**
   * Calculates and returns the set's size in bits. The maximum element in the
   * set is the size - 1st element.
   */
  public final int length() {
    return (bits.length << LOG_BITS_PER_UNIT) + offset;
  }

  /**
   * Sets all bits.
   */
  public final void setAll() {
    expand(0, length());
    for (int i = 0; i < bits.length; i++) {
      bits[i] = MASK;
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
    if ((obj != null) && (obj instanceof OffsetBitVector)) {
      if (this == obj) { // should help alias analysis
        return true;
      }
      OffsetBitVector set = (OffsetBitVector) obj;
      return sameBits(set);
    }
    return false;
  }

  /**
   * Check if the intersection of the two sets is empty
   * 
   * @param other
   *          the set to check intersection with
   */
  public final boolean intersectionEmpty(OffsetBitVector set) {
    if (this == set) {
      return isZero();
    }

    int wordDiff = wordDiff(offset, set.offset);
    int maxWord = Math.min(bits.length, set.bits.length-wordDiff);

    int i = Math.max(0, -wordDiff);

    for ( ; i < maxWord; i++) {
      if ((bits[i] & set.bits[i+wordDiff]) != 0) {
	return false;
      }
    }

    return true;
  }

  /**
   * Compares this object against the specified object.
   * 
   * @param B
   *          the object to compare with
   * @return true if the objects are the same; false otherwise.
   */
  public final boolean sameBits(OffsetBitVector B) {
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
  public boolean isSubset(OffsetBitVector other) {
    if (this == other) { // should help alias analysis
      return true;
    }
    int wordDiff = wordDiff(offset, other.offset);

    int i = 0;
    for ( ; i < -wordDiff; i++) {
      if (other.bits[i] != 0) {
        return false;
      }
    }

    int min = Math.min(bits.length, other.bits.length+wordDiff);
    for ( ; i < min; i++) {
      if ((bits[i] & ~other.bits[i-wordDiff]) != 0) {
        return false;
      }
    }

    for ( ; i < bits.length; i++) {
      if (bits[i] != 0) {
	return false;
      }
    }

    return true;
  }

  /**
   * Copies the values of the bits in the specified set into this set.
   * 
   * @param set
   *          the bit set to copy the bits from
   */
  public final void copyBits(OffsetBitVector set) {
    super.copyBits(set);
    offset = set.offset;
  }

  /**
   * Logically ANDs this bit set with the specified set of bits.
   * 
   * @param set
   *          the bit set to be ANDed with
   */
  public final void and(OffsetBitVector set) {
    if (this == set) {
      return;
    }

    int wordDiff = wordDiff(offset, set.offset);
    int maxWord = Math.min(bits.length, set.bits.length+wordDiff);

    int i = 0;

    for ( ; i < -wordDiff; i++) {
      bits[i] = 0;
    }

    for ( ; i < maxWord; i++) {
      bits[i] &= set.bits[i-wordDiff];
    }

    for ( ; i < bits.length; i++) {
      bits[i] = 0;
    }   
  }

  /**
   * Logically ORs this bit set with the specified set of bits.
   * 
   * @param set
   *          the bit set to be ORed with
   */
  public final void or(OffsetBitVector set) {
    if (this == set) { // should help alias analysis
      return;
    }

    int newOffset = Math.min(offset, set.offset);
    int newCapacity = 
      Math.max(length(),set.length())-newOffset;
    ensureCapacity(newOffset, newCapacity);

    int wordDiff = wordDiff(newOffset, set.offset);

    for (int i = 0; i < set.bits.length; i++) {
      bits[i-wordDiff] |= set.bits[i];
    }
  }

  /**
   * Logically XORs this bit set with the specified set of bits.
   * 
   * @param set
   *          the bit set to be XORed with
   */
  public final void xor(OffsetBitVector set) {
    if (this == set) {
      clearAll();
      return;
    }

    int newOffset = Math.min(offset, set.offset);
    int newCapacity = 
      Math.max(length(),set.length())-newOffset;
    ensureCapacity(newOffset, newCapacity);

    int wordDiff = wordDiff(newOffset, set.offset);

    for (int i = 0; i < set.bits.length; i++) {
      bits[i-wordDiff] ^= set.bits[i];
    }
  }

  /**
   * @param vector
   */
  public void andNot(OffsetBitVector set) {
    if (this == set) { // should help alias analysis
      return;
    }

    int newOffset = Math.min(offset, set.offset);
    int newCapacity = 
      Math.max(length(),set.length())-newOffset;
    ensureCapacity(newOffset, newCapacity);

    int wordDiff = wordDiff(offset, set.offset);

    int n = Math.min(bits.length, set.bits.length+wordDiff);
    for (int i = n - 1; i >= 0;) {
      bits[i] &= ~(set.bits[i-wordDiff]);
      i--;
    }
  }
  
  /**
   * Return the NOT of a bit string
   */
  public static OffsetBitVector not(OffsetBitVector s) {
    OffsetBitVector b = new OffsetBitVector(s);
    b.not();
    return b;
  }

  /**
   * Return a new bit string as the AND of two others.
   */
  public static OffsetBitVector and(OffsetBitVector b1, OffsetBitVector b2) {
    OffsetBitVector b = new OffsetBitVector(b1);
    b.and(b2);
    return b;
  }

  /**
   * Return a new FixedSizeBitVector as the OR of two others
   */
  public static OffsetBitVector or(OffsetBitVector b1, OffsetBitVector b2) {
    OffsetBitVector b = new OffsetBitVector(b1);
    b.or(b2);
    return b;
  }

  /**
   * Return a new bit string as the AND of two others.
   */
  public static OffsetBitVector andNot(OffsetBitVector b1, OffsetBitVector b2) {
    OffsetBitVector b = new OffsetBitVector(b1);
    b.andNot(b2);
    return b;
  }
}
