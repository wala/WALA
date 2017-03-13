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

import java.util.Iterator;

import com.ibm.wala.util.debug.Assertions;

/**
 * A bit set is a set of elements, each of which corresponds to a unique integer from [0,MAX].
 */
public final class BitSet<T> {

  /**
   * The backing bit vector that determines set membership.
   */
  final private BitVector vector;

  /**
   * The bijection between integer to object.
   */
  private OrdinalSetMapping<T> map;

  /**
   * Constructor: create an empty set corresponding to a given mapping
   * 
   * @throws IllegalArgumentException if map is null
   */
  public BitSet(OrdinalSetMapping<T> map) {
    if (map == null) {
      throw new IllegalArgumentException("map is null");
    }
    int length = map.getMaximumIndex();
    vector = new BitVector(length);
    this.map = map;
  }

  public static <T> BitSet<T> createBitSet(BitSet<T> B) {
    if (B == null) {
      throw new IllegalArgumentException("null B");
    }
    return new BitSet<>(B);
  }

  private BitSet(BitSet<T> B) {
    this(B.map);
    addAll(B);
  }

  /**
   * Add all elements in bitset B to this bit set
   * 
   * @throws IllegalArgumentException if B is null
   */
  public void addAll(BitSet<?> B) {
    if (B == null) {
      throw new IllegalArgumentException("B is null");
    }
    vector.or(B.vector);
  }

  /**
   * Add all bits in BitVector B to this bit set
   */
  public void addAll(BitVector B) {
    vector.or(B);
  }

  /**
   * Add an object to this bit set.
   */
  public void add(T o) {
    int n = map.getMappedIndex(o);
    vector.set(n);
  }

  /**
   * Remove an object from this bit set.
   * 
   * @param o the object to remove
   */
  public void clear(T o) {
    int n = map.getMappedIndex(o);
    if (n == -1) {
      return;
    }
    vector.clear(n);
  }

  /**
   * Does this set contain a certain object?
   */
  public boolean contains(T o) {
    int n = map.getMappedIndex(o);
    if (n == -1) {
      return false;
    }
    return vector.get(n);
  }

  /**
   * @return a String representation
   */
  @Override
  public String toString() {
    return vector.toString();
  }

  /**
   * Method copy. Copies the bits in the bit vector, but only assigns the object map. No need to create a new object/bit bijection
   * object.
   * 
   * @throws IllegalArgumentException if other is null
   */
  public void copyBits(BitSet<T> other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    vector.copyBits(other.vector);
    map = other.map;
  }

  /**
   * Does this object hold the same bits as other?
   * 
   * @throws IllegalArgumentException if other is null
   */
  public boolean sameBits(BitSet<?> other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    return vector.equals(other.vector);
  }

  /**
   * Not very efficient.
   */
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      private int nextCounter = -1;
      {
        for (int i = 0; i < vector.length(); i++) {
          if (vector.get(i)) {
            nextCounter = i;
            break;
          }
        }
      }

      @Override
      public boolean hasNext() {
        return (nextCounter != -1);
      }

      @Override
      public T next() {
        T result = map.getMappedObject(nextCounter);
        int start = nextCounter + 1;
        nextCounter = -1;
        for (int i = start; i < vector.length(); i++) {
          if (vector.get(i)) {
            nextCounter = i;
            break;
          }
        }
        return result;
      }

      @Override
      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  public int size() {
    return vector.populationCount();
  }

  public int length() {
    return vector.length();
  }

  /**
   * Set all the bits to 0.
   */
  public void clearAll() {
    vector.clearAll();
  }

  /**
   * Set all the bits to 1.
   */
  public void setAll() {
    vector.setAll();
  }

  /**
   * Perform intersection of two bitsets
   * 
   * @param other the other bitset in the operation
   * @throws IllegalArgumentException if other is null
   */
  public void intersect(BitSet<?> other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    vector.and(other.vector);
  }

  /**
   * Perform the difference of two bit sets
   * 
   * @param other the other bitset in the operation
   * @throws IllegalArgumentException if other is null
   */
  public void difference(BitSet<T> other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    vector.and(BitVector.not(other.vector));
  }

  /**
   */
  public boolean isEmpty() {
    return size() == 0;
  }

}
