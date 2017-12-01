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

public final class FixedSizeBitVector implements Cloneable, java.io.Serializable {
  public static final long serialVersionUID = 33181877746462822L;

  private final static int LOG_BITS_PER_UNIT = 5;

  private final static int MASK = 0xffffffff;

  private final static int LOW_MASK = 0x1f;

  private int bits[];

  final private int nbits;

  /**
   * Convert bitIndex to a subscript into the bits[] array.
   */
  private static int subscript(int bitIndex) {
    return bitIndex >> LOG_BITS_PER_UNIT;
  }

  /**
   * Creates an empty string with the specified size.
   * 
   * @param nbits the size of the string
   */
  public FixedSizeBitVector(int nbits) {
    // subscript(nbits) is the length of the array needed to
    // hold nbits
    if (nbits < 0) {
      throw new IllegalArgumentException("illegal nbits: " + nbits);
    }
    bits = new int[subscript(nbits) + 1];
    this.nbits = nbits;
  }

  /**
   * Creates a copy of a Bit String
   * 
   * @param s the string to copy
   * @throws IllegalArgumentException if s is null
   */
  public FixedSizeBitVector(FixedSizeBitVector s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    bits = new int[s.bits.length];
    this.nbits = s.nbits;
    copyBits(s);
  }

  /**
   * Sets all bits.
   */
  public void setAll() {
    for (int i = 0; i < bits.length; i++) {
      bits[i] = MASK;
    }
  }

  /**
   * Sets a bit.
   * 
   * @param bit the bit to be set
   */
  public void set(int bit) {
    try {
      int shiftBits = bit & LOW_MASK;
      bits[subscript(bit)] |= (1 << shiftBits);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid bit " + bit, e);
    }
  }

  /**
   * Clears all bits.
   */
  public void clearAll() {
    for (int i = 0; i < bits.length; i++) {
      bits[i] = 0;
    }
  }

  /**
   * Clears a bit.
   * 
   * @param bit the bit to be cleared
   */
  public void clear(int bit) {
    try {
      int shiftBits = bit & LOW_MASK;
      bits[subscript(bit)] &= ~(1 << shiftBits);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid bit: " + bit, e);
    }
  }

  /**
   * Gets a bit.
   * 
   * @param bit the bit to be gotten
   */
  public boolean get(int bit) {
    if (bit < 0) {
      throw new IllegalArgumentException("illegal bit: " + bit);
    }
    int shiftBits = bit & LOW_MASK;
    int n = subscript(bit);
    try {
      return ((bits[n] & (1 << shiftBits)) != 0);
    } catch (ArrayIndexOutOfBoundsException e) {
      return false;
    }
  }

  /**
   * Logically NOT this bit string
   */
  public void not() {
    for (int i = 0; i < bits.length; i++) {
      bits[i] ^= MASK;
    }
  }

  /**
   * Return the NOT of a bit string
   */
  public static FixedSizeBitVector not(FixedSizeBitVector s) {
    FixedSizeBitVector b = new FixedSizeBitVector(s);
    b.not();
    return b;
  }

  /**
   * Logically ANDs this bit set with the specified set of bits.
   * 
   * @param set the bit set to be ANDed with
   */
  public void and(FixedSizeBitVector set) {
    if (set == null) {
      throw new IllegalArgumentException("null set");
    }
    if (this == set) {
      return;
    }
    int n = bits.length;
    for (int i = n; i-- > 0;) {
      bits[i] &= set.bits[i];
    }
  }

  /**
   * Return a new bit string as the AND of two others.
   */
  public static FixedSizeBitVector and(FixedSizeBitVector b1, FixedSizeBitVector b2) {
    FixedSizeBitVector b = new FixedSizeBitVector(b1);
    b.and(b2);
    return b;
  }

  /**
   * Logically ORs this bit set with the specified set of bits.
   * 
   * @param set the bit set to be ORed with
   * @throws IllegalArgumentException if set == null
   */
  public void or(FixedSizeBitVector set) throws IllegalArgumentException {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    if (this == set) { // should help alias analysis
      return;
    }
    int setLength = set.bits.length;
    for (int i = setLength; i-- > 0;) {
      bits[i] |= set.bits[i];
    }
  }

  /**
   * Return a new FixedSizeBitVector as the OR of two others
   * 
   * @throws IllegalArgumentException if b2 == null
   */
  public static FixedSizeBitVector or(FixedSizeBitVector b1, FixedSizeBitVector b2) throws IllegalArgumentException {
    if (b2 == null) {
      throw new IllegalArgumentException("b2 == null");
    }
    FixedSizeBitVector b = new FixedSizeBitVector(b1);
    b.or(b2);
    return b;
  }

  /**
   * Logically XORs this bit set with the specified set of bits.
   * 
   * @param set the bit set to be XORed with
   * @throws IllegalArgumentException if set is null
   */
  public void xor(FixedSizeBitVector set) {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    int setLength = set.bits.length;
    for (int i = setLength; i-- > 0;) {
      bits[i] ^= set.bits[i];
    }
  }

  /**
   * Check if the intersection of the two sets is empty
   * 
   * @param other the set to check intersection with
   */
  public boolean intersectionEmpty(FixedSizeBitVector other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    int n = bits.length;
    for (int i = n; i-- > 0;) {
      if ((bits[i] & other.bits[i]) != 0)
        return false;
    }
    return true;
  }

  /**
   * Copies the values of the bits in the specified set into this set.
   * 
   * @param set the bit set to copy the bits from
   * @throws IllegalArgumentException if set is null
   */
  public void copyBits(FixedSizeBitVector set) {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    int setLength = set.bits.length;
    for (int i = setLength; i-- > 0;) {
      bits[i] = set.bits[i];
    }
  }

  /**
   * Gets the hashcode.
   */
  @Override
  public int hashCode() {
    int h = 1234;
    for (int i = bits.length; --i >= 0;) {
      h ^= bits[i] * (i + 1);
    }
    return h;
  }

  /**
   * How many bits are set?
   */
  public int populationCount() {
    int count = 0;
    for (int bit : bits) {
      count += Bits.populationCount(bit);
    }
    return count;
  }

  /**
   * Calculates and returns the set's size in bits. The maximum element in the set is the size - 1st element.
   */
  public int length() {
    return bits.length << LOG_BITS_PER_UNIT;
  }

  /**
   * Compares this object against the specified object.
   * 
   * @param obj the object to compare with
   * @return true if the objects are the same; false otherwise.
   */
  @Override
  public boolean equals(Object obj) {
    if ((obj != null) && (obj instanceof FixedSizeBitVector)) {
      if (this == obj) { // should help alias analysis
        return true;
      }
      FixedSizeBitVector set = (FixedSizeBitVector) obj;
      int n = bits.length;
      if (n != set.bits.length)
        return false;
      for (int i = n; i-- > 0;) {
        if (bits[i] != set.bits[i]) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  public boolean isZero() {
    int setLength = bits.length;
    for (int i = setLength; i-- > 0;) {
      if (bits[i] != 0)
        return false;
    }
    return true;
  }

  /**
   * Clones the FixedSizeBitVector.
   */
  @Override
  public Object clone() {
    FixedSizeBitVector result = null;
    try {
      result = (FixedSizeBitVector) super.clone();
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
  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    boolean needSeparator = false;
    buffer.append('{');
    // int limit = length();
    int limit = this.nbits;
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
}
