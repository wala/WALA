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

import com.ibm.wala.util.collections.CompoundIntIterator;
import com.ibm.wala.util.collections.EmptyIntIterator;

public class SemiSparseMutableIntSet implements MutableIntSet {
  private static final boolean DEBUG = true;

  private static final double FIX_SPARSE_MOD = 12;

  private static final double FIX_SPARSE_RATIO = .05;

  private MutableSparseIntSet sparsePart;

  private OffsetBitVector densePart = null;

  public SemiSparseMutableIntSet() {
    this(MutableSparseIntSet.makeEmpty());
  }

  private SemiSparseMutableIntSet(MutableSparseIntSet sparsePart) {
    this.sparsePart = sparsePart;
  }

  private SemiSparseMutableIntSet(MutableSparseIntSet sparsePart, OffsetBitVector densePart) {
    this.sparsePart = sparsePart;
    this.densePart = densePart;
  }

  public SemiSparseMutableIntSet(SemiSparseMutableIntSet set) throws IllegalArgumentException {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    copySet(set);
  }

  private final boolean assertDisjoint() {
    if (DEBUG) {
      if (densePart != null) {
        for (IntIterator sparseBits = sparsePart.intIterator(); sparseBits.hasNext();) {
          int bit = sparseBits.next();
          if (densePart.contains(bit)) {
            return false;
          }
          if (inDenseRange(bit)) {
            return false;
          }
        }
      }
    }

    return true;
  }

  private void fixAfterSparseInsert() {
    if (sparsePart.size() % FIX_SPARSE_MOD == FIX_SPARSE_MOD - 1
        && (densePart == null || (densePart != null && sparsePart.size() > FIX_SPARSE_RATIO * densePart.getSize()))) {
      assert assertDisjoint() : this.toString();

      if (densePart == null) {
        IntIterator sparseBits = sparsePart.intIterator();

        int maxOffset = -1;
        int maxCount = -1;
        int maxMax = -1;

        int offset = 0;
        int bits = 0;
        int count = 0;
        int oldBit = 0;
        while (sparseBits.hasNext()) {
          int nextBit = sparseBits.next();

          int newBits = bits + (nextBit - oldBit);
          int newCount = count + 1;

          if (newBits < (32 * newCount)) {
            count = newCount;
            bits = newBits;
            if (count > maxCount) {
              maxOffset = offset;
              maxMax = nextBit;
              maxCount = count;
            }
          } else {
            offset = nextBit;
            count = 1;
            bits = 32;
          }
          oldBit = nextBit;
        }

        if (maxOffset != -1) {
          densePart = new OffsetBitVector(maxOffset, maxMax - maxOffset);
          sparseBits = sparsePart.intIterator();
          int bit;
          while ((bit = sparseBits.next()) < maxOffset) {
            // do nothing
          }

          densePart.set(bit);
          for (int i = 1; i < maxCount; i++) {
            densePart.set(sparseBits.next());
          }
          sparsePart.removeAll(densePart);
        }

        assert assertDisjoint() : this.toString() + ", maxOffset=" + maxOffset + ", maxMax=" + maxMax + ", maxCount=" + maxCount;

      } else {
        IntIterator sparseBits = sparsePart.intIterator();
        int thisBit = sparseBits.next();

        int moveCount = 0;
        int newOffset = -1;
        int newCount = -1;
        int newLength = -1;

        // push stuff just below dense part into it, if it saves space
        if (thisBit < densePart.getOffset()) {
          newOffset = thisBit;
          int bits = 32;
          int count = 1;
          while (sparseBits.hasNext()) {
            int nextBit = sparseBits.next();
            if (nextBit >= densePart.getOffset() || !sparseBits.hasNext()) {
              if (nextBit < densePart.getOffset() && !sparseBits.hasNext()) {
                count++;
              }
              if (densePart.getOffset() - newOffset < (32 * count)) {
                moveCount += count;
              } else {
                newOffset = -1;
              }
              thisBit = nextBit;
              break;
            } else {
              bits += (nextBit - thisBit);
              count++;

              if (bits > (32 * count)) {
                newOffset = nextBit;
                count = 1;
                bits = 32;
              }

              thisBit = nextBit;
            }
          }
        }

        while (thisBit < densePart.length() && sparseBits.hasNext()) {
          thisBit = sparseBits.next();
        }

        // push stuff just above dense part into it, if it saves space
        if (thisBit >= densePart.length()) {
          int count = 1;
          int bits = (thisBit + 1 - densePart.length());
          if (32 * count > bits) {
            newLength = thisBit;
            newCount = 1;
          }
          while (sparseBits.hasNext()) {
            thisBit = sparseBits.next();
            count++;
            bits = (thisBit + 1 - densePart.length());
            if ((32 * count) > bits) {
              newLength = thisBit;
              newCount = count;
            }
          }
          if (newLength > -1) {
            moveCount += newCount;
          }
        }

        // actually move bits from sparse to dense
        if (moveCount > 0) {
          int index = 0;
          int[] bits = new int[moveCount];
          for (sparseBits = sparsePart.intIterator(); sparseBits.hasNext();) {
            int bit = sparseBits.next();
            if (newOffset != -1 && bit >= newOffset && bit < densePart.getOffset()) {
              bits[index++] = bit;
            }
            if (newLength != -1 && bit >= densePart.length() && bit <= newLength) {
              bits[index++] = bit;
            }
          }

          if (index != moveCount) {
            assert index == moveCount : "index is " + index + ", but moveCount is " + moveCount + " for " + this;
          }

          if (newLength != -1 && bits[index - 1] == sparsePart.max()) {
            int base = densePart.getOffset();
            int currentSize = densePart.length() - base;
            float newSize = 1.1f * (bits[index - 1] - base);
            float fraction = newSize / currentSize;
            assert fraction > 1;
            densePart.growCapacity(fraction);
          }

          for (int i = index - 1; i >= 0; i--) {
            sparsePart.remove(bits[i]);
            densePart.set(bits[i]);
          }
        }

        assert assertDisjoint() : this.toString() + ", densePart.length()=" + densePart.length() + ", newOffset=" + newOffset
            + ", newLength=" + newLength + ", newCount=" + newCount + ", moveCount=" + moveCount;
      }
    }
  }

  /* 
   * @see com.ibm.wala.util.intset.MutableIntSet#clear()
   */
  @Override
  public void clear() {
    sparsePart.clear();
    densePart = null;
  }
  
  /**
   * @param i
   * @return true iff this set contains integer i
   */
  @Override
  public boolean contains(int i) {
    if (densePart != null && inDenseRange(i)) {
      return densePart.contains(i);
    } else {
      return sparsePart.contains(i);
    }
  }

  /**
   * @return true iff this set contains integer i
   */
  @Override
  public boolean containsAny(IntSet set) {
    if (set == null) {
      throw new IllegalArgumentException("null set");
    }
    if (!sparsePart.isEmpty() && sparsePart.containsAny(set)) {
      return true;
    } else if (densePart != null) {
      int lower = densePart.getOffset();
      for (IntIterator is = set.intIterator(); is.hasNext();) {
        int i = is.next();
        if (i < lower)
          continue;
        if (densePart.get(i)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * This implementation must not despoil the original value of "this"
   * 
   * @return a new IntSet which is the intersection of this and that
   */
  @Override
  public IntSet intersection(IntSet that) {
    if (that == null) {
      throw new IllegalArgumentException("null that");
    }
    SemiSparseMutableIntSet newThis = new SemiSparseMutableIntSet();
    for (IntIterator bits = intIterator(); bits.hasNext();) {
      int bit = bits.next();
      if (that.contains(bit)) {
        newThis.add(bit);
      }
    }
    return newThis;
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#union(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public IntSet union(IntSet that) {
    SemiSparseMutableIntSet temp = new SemiSparseMutableIntSet();
    temp.addAll(this);
    temp.addAll(that);

    return temp;
  }

  /**
   * @return true iff this set is empty
   */
  @Override
  public boolean isEmpty() {
    return sparsePart.isEmpty() && (densePart == null || densePart.isZero());
  }

  /**
   * @return the number of elements in this set
   */
  @Override
  public int size() {
    return sparsePart.size() + (densePart == null ? 0 : densePart.populationCount());
  }

  /**
   * @return a perhaps more efficient iterator
   */
  @Override
  public IntIterator intIterator() {
    class DensePartIterator implements IntIterator {
      private int i = -1;

      @Override
      public boolean hasNext() {
        return densePart.nextSetBit(i + 1) != -1;
      }

      @Override
      public int next() {
        int next = densePart.nextSetBit(i + 1);
        i = next;
        return next;
      }
    }

    if (sparsePart.isEmpty()) {
      if (densePart == null || densePart.isZero()) {
        return EmptyIntIterator.instance();
      } else {
        return new DensePartIterator();
      }
    } else {
      if (densePart == null || densePart.isZero()) {
        return sparsePart.intIterator();
      } else {
        return new CompoundIntIterator(sparsePart.intIterator(), new DensePartIterator());
      }
    }
  }

  /**
   * Invoke an action on each element of the Set
   */
  @Override
  public void foreach(IntSetAction action) {
    if (action == null) {
      throw new IllegalArgumentException("null action");
    }
    sparsePart.foreach(action);
    if (densePart != null) {
      for (int b = densePart.nextSetBit(0); b != -1; b = densePart.nextSetBit(b + 1)) {
        action.act(b);
      }
    }
  }

  /**
   * Invoke an action on each element of the Set, excluding elements of Set X
   * 
   * @param action
   */
  @Override
  public void foreachExcluding(IntSet X, IntSetAction action) {
    sparsePart.foreachExcluding(X, action);
    if (densePart != null) {
      for (int b = densePart.nextSetBit(0); b != -1; b = densePart.nextSetBit(b + 1)) {
        if (!X.contains(b)) {
          action.act(b);
        }
      }
    }
  }

  /**
   * @return maximum integer in this set.
   */
  @Override
  public int max() throws IllegalStateException {
    if (densePart == null) {
      return sparsePart.max();
    } else {
      return Math.max(sparsePart.max(), densePart.max());
    }
  }

  /**
   * @return true iff <code>this</code> has the same value as <code>that</code>.
   * @throws IllegalArgumentException if that is null
   */
  @Override
  public boolean sameValue(IntSet that) {
    if (that == null) {
      throw new IllegalArgumentException("that is null");
    }
    if (size() != that.size()) {
      return false;
    }
    if (densePart != null) {
      for (int bit = densePart.nextSetBit(0); bit != -1; bit = densePart.nextSetBit(bit + 1)) {
        if (!that.contains(bit)) {
          return false;
        }
      }
    }
    for (IntIterator bits = sparsePart.intIterator(); bits.hasNext();) {
      if (!that.contains(bits.next())) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return true iff <code>this</code> is a subset of <code>that</code>.
   * @throws IllegalArgumentException if that is null
   */
  @Override
  public boolean isSubset(IntSet that) {
    if (that == null) {
      throw new IllegalArgumentException("that is null");
    }
    if (size() > that.size()) {
      return false;
    }

    for (IntIterator bits = sparsePart.intIterator(); bits.hasNext();) {
      if (!that.contains(bits.next())) {
        return false;
      }
    }

    if (densePart != null) {
      for (int b = densePart.nextSetBit(0); b != -1; b = densePart.nextSetBit(b + 1)) {
        if (!that.contains(b)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Set the value of this to be the same as the value of set
   * 
   * @throws IllegalArgumentException if set == null
   */
  @Override
  public void copySet(IntSet set) throws IllegalArgumentException {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    if (set instanceof SemiSparseMutableIntSet) {
      SemiSparseMutableIntSet that = (SemiSparseMutableIntSet) set;
      sparsePart = MutableSparseIntSet.make(that.sparsePart);
      if (that.densePart == null) {
        densePart = null;
      } else {
        densePart = new OffsetBitVector(that.densePart);
      }
    } else {
      densePart = null;
      sparsePart = MutableSparseIntSet.makeEmpty();
      for (IntIterator bits = set.intIterator(); bits.hasNext();) {
        add(bits.next());
      }
    }
  }

  private boolean inDenseRange(int i) {
    return densePart.getOffset() <= i && densePart.length() > i;
  }

  /**
   * Add all members of set to this.
   * 
   * @return true iff the value of this changes.
   * @throws IllegalArgumentException if set == null
   */
  @Override
  public boolean addAll(IntSet set) throws IllegalArgumentException {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    boolean change = false;
    if (set instanceof SemiSparseMutableIntSet) {
      SemiSparseMutableIntSet that = (SemiSparseMutableIntSet) set;

      if (densePart == null) {

        // that dense part only
        if (that.densePart != null) {
          int oldSize = size();
          densePart = new OffsetBitVector(that.densePart);

          for (IntIterator bits = sparsePart.intIterator(); bits.hasNext();) {
            int bit = bits.next();
            if (inDenseRange(bit)) {
              densePart.set(bit);
            }
          }

          sparsePart.removeAll(densePart);
          sparsePart.addAll(that.sparsePart);

          change = size() != oldSize;

          // no dense part
        } else {
          change = sparsePart.addAll(that.sparsePart);
          fixAfterSparseInsert();
        }

      } else {
        // both dense parts
        if (that.densePart != null) {
          int oldSize = size();

          densePart.or(that.densePart);

          sparsePart.addAll(that.sparsePart);

          for (IntIterator bits = sparsePart.intIterator(); bits.hasNext();) {
            int bit = bits.next();
            if (inDenseRange(bit)) {
              densePart.set(bit);
            }
          }

          sparsePart.removeAll(densePart);

          change = size() != oldSize;

          // this dense part only
        } else {
          for (IntIterator bs = that.sparsePart.intIterator(); bs.hasNext();) {
            change |= add(bs.next());
          }
        }
      }
    } else {
      for (IntIterator bs = set.intIterator(); bs.hasNext();) {
        change |= add(bs.next());
      }
    }

    assert assertDisjoint() : this.toString();

    return change;
  }

  /**
   * Add an integer value to this set.
   * 
   * @param i integer to add
   * @return true iff the value of this changes.
   */
  @Override
  public boolean add(int i) {
    if (densePart != null && inDenseRange(i)) {
      if (!densePart.get(i)) {
        densePart.set(i);
        assert assertDisjoint() : this.toString();
        return true;
      }
    } else if (!sparsePart.contains(i)) {
      sparsePart.add(i);
      assert assertDisjoint() : this.toString();
      fixAfterSparseInsert();
      return true;
    }

    return false;
  }

  /**
   * Remove an integer from this set.
   * 
   * @param i integer to remove
   * @return true iff the value of this changes.
   */
  @Override
  public boolean remove(int i) {
    if (densePart != null && densePart.get(i)) {
      densePart.clear(i);
      if (densePart.nextSetBit(0) == -1) {
        densePart = null;
      }
      return true;
    } else if (sparsePart.contains(i)) {
      sparsePart.remove(i);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Interset this with another set.
   * 
   * @param set
   */
  @Override
  public void intersectWith(IntSet set) {
    sparsePart.intersectWith(set);
    if (densePart != null) {
      for (int b = densePart.nextSetBit(0); b != -1; b = densePart.nextSetBit(b + 1)) {
        if (!set.contains(b)) {
          densePart.clear(b);
        }
      }
    }
  }

  /**
   * @throws IllegalArgumentException if other is null
   */
  @Override
  public boolean addAllInIntersection(IntSet other, IntSet filter) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    if (filter == null) {
      throw new IllegalArgumentException("null filter");
    }
    boolean change = false;
    for (IntIterator bits = other.intIterator(); bits.hasNext();) {
      int bit = bits.next();
      if (filter.contains(bit)) {
        change |= add(bit);
      }
    }

    return change;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("[");
    if (densePart != null) {
      sb.append("densePart: ").append(densePart.toString()).append(" ");
    }
    sb.append("sparsePart: ").append(sparsePart.toString()).append("]");
    return sb.toString();
  }

  public SemiSparseMutableIntSet removeAll(SemiSparseMutableIntSet B) {
    if (B == null) {
      throw new IllegalArgumentException("B null");
    }
    if (densePart == null) {
      if (B.densePart == null) {
        sparsePart = MutableSparseIntSet.diff(sparsePart, B.sparsePart);
      } else {
        MutableSparseIntSet C = MutableSparseIntSet.diff(sparsePart, B.sparsePart);
        for (IntIterator bits = sparsePart.intIterator(); bits.hasNext();) {
          int bit = bits.next();
          if (B.densePart.get(bit)) {
            C.remove(bit);
          }
        }
        sparsePart = C;
      }
    } else {
      if (B.densePart == null) {
        for (IntIterator bits = B.sparsePart.intIterator(); bits.hasNext();) {
          densePart.clear(bits.next());
        }
        sparsePart = MutableSparseIntSet.diff(sparsePart, B.sparsePart);
      } else {
        densePart.andNot(B.densePart);
        for (IntIterator bits = B.sparsePart.intIterator(); bits.hasNext();) {
          densePart.clear(bits.next());
        }

        MutableSparseIntSet C = MutableSparseIntSet.diff(sparsePart, B.sparsePart);
        for (IntIterator bits = sparsePart.intIterator(); bits.hasNext();) {
          int bit = bits.next();
          if (B.densePart.get(bit)) {
            C.remove(bit);
          }
        }

        sparsePart = C;
      }
    }

    return this;
  }

  public static SemiSparseMutableIntSet diff(SemiSparseMutableIntSet A, SemiSparseMutableIntSet B) {
    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    if (B == null) {
      throw new IllegalArgumentException("B is null");
    }
    if (A.densePart == null) {
      if (B.densePart == null) {
        return new SemiSparseMutableIntSet(MutableSparseIntSet.diff(A.sparsePart, B.sparsePart));

      } else {
        MutableSparseIntSet C = MutableSparseIntSet.diff(A.sparsePart, B.sparsePart);
        for (IntIterator bits = A.sparsePart.intIterator(); bits.hasNext();) {
          int bit = bits.next();
          if (B.densePart.get(bit)) {
            C.remove(bit);
          }
        }

        return new SemiSparseMutableIntSet(C);
      }

    } else {
      if (B.densePart == null) {
        OffsetBitVector newDensePart = new OffsetBitVector(A.densePart);
        for (IntIterator bits = B.sparsePart.intIterator(); bits.hasNext();) {
          newDensePart.clear(bits.next());
        }

        return new SemiSparseMutableIntSet(MutableSparseIntSet.diff(A.sparsePart, B.sparsePart), newDensePart);

      } else {
        OffsetBitVector newDensePart = new OffsetBitVector(A.densePart);
        newDensePart.andNot(B.densePart);
        for (IntIterator bits = B.sparsePart.intIterator(); bits.hasNext();) {
          newDensePart.clear(bits.next());
        }

        MutableSparseIntSet C = MutableSparseIntSet.diff(A.sparsePart, B.sparsePart);
        for (IntIterator bits = A.sparsePart.intIterator(); bits.hasNext();) {
          int bit = bits.next();
          if (B.densePart.get(bit)) {
            C.remove(bit);
          }
        }

        return new SemiSparseMutableIntSet(C, newDensePart);
      }
    }
  }

}
