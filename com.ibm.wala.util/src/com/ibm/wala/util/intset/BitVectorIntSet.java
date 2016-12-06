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
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * A {@link BitVector} implementation of {@link MutableIntSet}.
 * 
 * Note that this is NOT a value with regard to hashCode and equals.
 */
public final class BitVectorIntSet implements MutableIntSet {

  // population count of -1 means needs to be computed again.
  private int populationCount = 0;

  private static final int UNDEFINED = -1;

  private BitVector bitVector = new BitVector(0);

  public BitVectorIntSet() {
  }

  public BitVectorIntSet(BitVector v) {
    if (v == null) {
      throw new IllegalArgumentException("null v");
    }
    bitVector.or(v);
    populationCount = UNDEFINED;
  }

  public BitVectorIntSet(IntSet S) throws IllegalArgumentException {
    if (S == null) {
      throw new IllegalArgumentException("S == null");
    }
    copySet(S);
  }

  /* 
   * @see com.ibm.wala.util.intset.MutableIntSet#clear()
   */
  @Override
  public void clear() {
    bitVector.clearAll();
    populationCount = 0;
  }
  
  /*
   * @see com.ibm.wala.util.intset.MutableIntSet#copySet(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public void copySet(IntSet set) throws IllegalArgumentException {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    if (set instanceof BitVectorIntSet) {
      BitVectorIntSet S = (BitVectorIntSet) set;
      bitVector = new BitVector(S.bitVector);
      populationCount = S.populationCount;
    } else if (set instanceof MutableSharedBitVectorIntSet) {
      BitVectorIntSet S = ((MutableSharedBitVectorIntSet) set).makeDenseCopy();
      bitVector = new BitVector(S.bitVector);
      populationCount = S.populationCount;
    } else if (set instanceof SparseIntSet) {
      SparseIntSet s = (SparseIntSet) set;
      if (s.size == 0) {
        populationCount = 0;
        bitVector = new BitVector(0);
      } else {
        bitVector = new BitVector(s.max());
        populationCount = s.size;
        for (int i = 0; i < s.size; i++) {
          bitVector.set(s.elements[i]);
        }
      }
    } else if (set instanceof BimodalMutableIntSet) {
      IntSet backing = ((BimodalMutableIntSet) set).getBackingStore();
      copySet(backing);
    } else {
      bitVector.clearAll();
      populationCount = set.size();
      for (IntIterator it = set.intIterator(); it.hasNext();) {
        bitVector.set(it.next());
      }
    }

  }

  @Override
  public boolean addAll(IntSet set) {
    if (set instanceof BitVectorIntSet) {
      BitVector B = ((BitVectorIntSet) set).bitVector;
      int delta = bitVector.orWithDelta(B);
      populationCount += delta;
      populationCount = (populationCount == (delta + UNDEFINED)) ? UNDEFINED : populationCount;
      return (delta != 0);
    } else {
      BitVectorIntSet other = new BitVectorIntSet(set);
      return addAll(other);
    }
  }

  /**
   * this version of add all will likely be faster if the client doesn't care about the change or the population count.
   * @param set
   * @throws IllegalArgumentException if set == null
   */
  public void addAllOblivious(IntSet set) throws IllegalArgumentException {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    if (set instanceof BitVectorIntSet) {
      BitVector B = ((BitVectorIntSet) set).bitVector;
      bitVector.or(B);
      populationCount = UNDEFINED;
    } else {
      BitVectorIntSet other = new BitVectorIntSet(set);
      addAllOblivious(other);
    }
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSet#add(int)
   */
  @Override
  public boolean add(int i) {
    if (bitVector.get(i)) {
      return false;
    } else {
      bitVector.set(i);
      populationCount++;
      populationCount = (populationCount == (UNDEFINED + 1)) ? UNDEFINED : populationCount;
      return true;
    }
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSet#remove(int)
   */
  @Override
  public boolean remove(int i) {
    if (contains(i)) {
      populationCount--;
      populationCount = (populationCount == UNDEFINED - 1) ? UNDEFINED : populationCount;
      bitVector.clear(i);
      return true;
    } else {
      return false;
    }
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSet#intersectWith(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public void intersectWith(IntSet set) {
    if (!(set instanceof BitVectorIntSet)) {
      set = new BitVectorIntSet(set);
    }
    BitVector B = ((BitVectorIntSet) set).bitVector;
    bitVector.and(B);
    populationCount = UNDEFINED;
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#intersection(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public BitVectorIntSet intersection(IntSet that) {
    BitVectorIntSet newbie = new BitVectorIntSet();
    newbie.copySet(this);
    newbie.intersectWith(that);
    return newbie;
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#union(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public IntSet union(IntSet that) {
    BitVectorIntSet temp = new BitVectorIntSet();
    temp.addAll(this);
    temp.addAll(that);

    return temp;
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#size()
   */
  @Override
  public int size() {
    populationCount = (populationCount == UNDEFINED) ? bitVector.populationCount() : populationCount;
    return populationCount;
  }

  /**
   * Use with extreme care; doesn't detect ConcurrentModificationExceptions
   */
  @Override
  public IntIterator intIterator() {
    populationCount = (populationCount == UNDEFINED) ? bitVector.populationCount() : populationCount;
    return new IntIterator() {
      int count = 0;

      int last = 0;

      @Override
      public boolean hasNext() {
        assert populationCount == bitVector.populationCount();
        return count < populationCount;
      }

      @Override
      public int next() {
        count++;
        last = nextSetBit(last) + 1;
        return last - 1;
      }
    };
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#foreach(com.ibm.wala.util.intset.IntSetAction)
   */
  @Override
  public void foreach(IntSetAction action) {
    if (action == null) {
      throw new IllegalArgumentException("null action");
    }
    int nextBit = bitVector.nextSetBit(0);
    populationCount = (populationCount == UNDEFINED) ? bitVector.populationCount() : populationCount;
    for (int i = 0; i < populationCount; i++) {
      action.act(nextBit);
      nextBit = bitVector.nextSetBit(nextBit + 1);
    }
  }

  public SparseIntSet makeSparseCopy() {
    populationCount = (populationCount == UNDEFINED) ? bitVector.populationCount() : populationCount;
    int[] elements = new int[populationCount];
    int i = 0;
    int nextBit = -1;
    while (i < populationCount)
      elements[i++] = nextBit = bitVector.nextSetBit(nextBit + 1);

    return new SparseIntSet(elements);
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#foreach(com.ibm.wala.util.intset.IntSetAction)
   */
  @Override
  public void foreachExcluding(IntSet X, IntSetAction action) {
    if (X instanceof BitVectorIntSet) {
      fastForeachExcluding((BitVectorIntSet) X, action);
    } else {
      slowForeachExcluding(X, action);
    }
  }

  private void slowForeachExcluding(IntSet X, IntSetAction action) {
    populationCount = (populationCount == UNDEFINED) ? bitVector.populationCount() : populationCount;
    for (int i = 0, count = 0; count < populationCount; i++) {
      if (contains(i)) {
        if (!X.contains(i)) {
          action.act(i);
        }
        count++;
      }
    }
  }

  /**
   * internal optimized form
   * 
   * @param X
   * @param action
   */
  private void fastForeachExcluding(BitVectorIntSet X, IntSetAction action) {
    int[] bits = bitVector.bits;
    int[] xbits = X.bitVector.bits;

    int w = 0;
    while (w < xbits.length && w < bits.length) {
      int b = bits[w] & ~xbits[w];
      actOnWord(action, w << 5, b);
      w++;
    }
    while (w < bits.length) {
      actOnWord(action, w << 5, bits[w]);
      w++;
    }
  }

  private void actOnWord(IntSetAction action, int startingIndex, int word) {
    if (word != 0) {
      if ((word & 0x1) != 0) {
        action.act(startingIndex);
      }
      for (int i = 0; i < 31; i++) {
        startingIndex++;
        word = word >>> 1;
        if ((word & 0x1) != 0) {
          action.act(startingIndex);
        }
      }
    }
  }

  @Override
  public boolean contains(int i) {
    if (i < 0) {
      throw new IllegalArgumentException("invalid i: " + i)  ;
    }
    return bitVector.get(i);
  }

  @Override
  public int max() {
    return bitVector.max();
  }

  @Override
  public String toString() {
    return bitVector.toString();
  }

  /**
   * @return min j &gt;= n s.t get(j)
   */
  public int nextSetBit(int n) {
    return bitVector.nextSetBit(n);
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#sameValue(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public boolean sameValue(IntSet that) throws IllegalArgumentException, UnimplementedError {
    if (that == null) {
      throw new IllegalArgumentException("that == null");
    }
    if (that instanceof BitVectorIntSet) {
      BitVectorIntSet b = (BitVectorIntSet) that;
      return bitVector.sameBits(b.bitVector);
    } else if (that instanceof BimodalMutableIntSet) {
      return that.sameValue(this);
    } else if (that instanceof SparseIntSet) {
      return sameValueInternal((SparseIntSet) that);
    } else if (that instanceof MutableSharedBitVectorIntSet) {
      return sameValue(((MutableSharedBitVectorIntSet) that).makeDenseCopy());
    } else {
      Assertions.UNREACHABLE("unexpected argument type " + that.getClass());
      return false;
    }
  }

  /**
   */
  private boolean sameValueInternal(SparseIntSet that) {
    populationCount = (populationCount == UNDEFINED) ? bitVector.populationCount() : populationCount;
    if (populationCount != that.size()) {
      return false;
    }
    for (int i = 0; i < that.size(); i++) {
      int val = that.elementAt(i);
      if (!bitVector.contains(val)) {
        return false;
      }
    }
    return true;
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#isSubset(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public boolean isSubset(IntSet that) {
    if (that instanceof BitVectorIntSet) {
      return bitVector.isSubset(((BitVectorIntSet) that).bitVector);
    } else if (that instanceof SparseIntSet) {
      return isSubsetInternal((SparseIntSet) that);
    } else {
      // really slow. optimize as needed.
      for (IntIterator it = intIterator(); it.hasNext();) {
        int x = it.next();
        if (!that.contains(x)) {
          return false;
        }
      }
      return true;
    }
  }

  private boolean isSubsetInternal(SparseIntSet set) {
    return toSparseIntSet().isSubset(set);
  }

  public BitVector getBitVector() {
    return bitVector;
  }

  /**
   * TODO: optimize
   * 
   */
  public SparseIntSet toSparseIntSet() {
    MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
    for (IntIterator it = intIterator(); it.hasNext();) {
      result.add(it.next());
    }
    return result;
  }

  /**
   * @param set
   * @throws IllegalArgumentException if set is null
   */
  public boolean removeAll(BitVectorIntSet set) {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    int oldSize = size();
    bitVector.andNot(set.bitVector);
    populationCount = UNDEFINED;
    return oldSize > size();
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#containsAny(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public boolean containsAny(IntSet set) throws IllegalArgumentException {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    if (set instanceof BitVectorIntSet) {
      BitVectorIntSet b = (BitVectorIntSet) set;
      return !bitVector.intersectionEmpty(b.bitVector);
    } else {
      // TODO: optimize
      for (IntIterator it = set.intIterator(); it.hasNext();) {
        if (contains(it.next())) {
          return true;
        }
      }
      return false;
    }
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSet#addAllInIntersection(com.ibm.wala.util.intset.IntSet,
   *      com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public boolean addAllInIntersection(IntSet other, IntSet filter) throws IllegalArgumentException {
    if (other == null) {
      throw new IllegalArgumentException("other == null");
    }
    BitVectorIntSet o = new BitVectorIntSet(other);
    o.intersectWith(filter);
    return addAll(o);
  }

  public boolean containsAll(BitVectorIntSet other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    return other.isSubset(this);
  }
}
