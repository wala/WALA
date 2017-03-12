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
 * A sparse ordered, mutable duplicate-free, fully-encapsulated set of longs. Instances are not canonical, except for EMPTY.
 * 
 * This implementation will be inefficient if these sets get large.
 * 
 * TODO: even for small sets, we probably want to work on this to reduce the allocation activity.
 */
public final class MutableSparseLongSet extends SparseLongSet implements MutableLongSet {

  /**
   * If forced to grow the backing array .. then by how much
   */
  private final static float EXPANSION_FACTOR = 1.5f;

  /**
   * Default initial size for a backing array with one element
   */
  private final static int INITIAL_NONEMPTY_SIZE = 2;

  public static MutableSparseLongSet make(LongSet set) throws UnimplementedError {
    if (!(set instanceof SparseLongSet)) {
      Assertions.UNREACHABLE("implement me");
    }
    return new MutableSparseLongSet(set);
  }

  public static MutableSparseLongSet createMutableSparseLongSet(int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("illegal initialCapacity: " + initialCapacity);
    }
    return new MutableSparseLongSet(initialCapacity);
  }

  private MutableSparseLongSet(LongSet set) {
    super();
    copySet(set);
  }

  public MutableSparseLongSet(long[] backingStore) {
    super(backingStore);
  }

  /**
   * Create an empty set with a non-zero capacity
   */
  private MutableSparseLongSet(int initialCapacity) {
    super(new long[initialCapacity]);
    size = 0;
  }

  public MutableSparseLongSet() {
    super();
  }

  /**
   */
  @Override
  public void remove(long value) {
    if (elements != null) {
      int remove;
      for (remove = 0; remove < size; remove++) {
        if (elements[remove] >= value) {
          break;
        }
      }
      if (remove == size) {
        return;
      }
      if (elements[remove] == value) {
        if (size == 1) {
          elements = null;
          size = 0;
        } else {
          if (remove < size) {
            System.arraycopy(elements, remove + 1, elements, remove, size - remove - 1);
          }
          size--;
        }
      }
    }
  }

  /**
   * @param value
   * @return true iff this value changes
   */
  @Override
  public boolean add(long value) {
    if (value < 0) {
      throw new IllegalArgumentException("illegal value: " + value);
    }
    if (elements == null) {
      elements = new long[INITIAL_NONEMPTY_SIZE];
      size = 1;
      elements[0] = value;
      return true;
    } else {
      int insert;
      if (size == 0 || value > max()) {
        insert = size;
      } else if (value == max()) {
        return false;
      } else {
        for (insert = 0; insert < size; insert++) {
          if (elements[insert] >= value) {
            break;
          }
        }
      }
      if (insert < size && elements[insert] == value) {
        return false;
      }
      if (size < elements.length - 1) {
        // there's space in the backing elements array. Use it.
        if (size != insert) {
          System.arraycopy(elements, insert, elements, insert + 1, size - insert);
        }
        size++;
        elements[insert] = value;
        return true;
      } else {
        // no space left. expand the backing array.
        float newExtent = elements.length * EXPANSION_FACTOR + 1;
        long[] tmp = new long[(int) newExtent];
        System.arraycopy(elements, 0, tmp, 0, insert);
        if (size != insert) {
          System.arraycopy(elements, insert, tmp, insert + 1, size - insert);
        }
        tmp[insert] = value;
        size++;
        elements = tmp;
        return true;
      }
    }
  }

  /**
   * @throws UnimplementedError if not ( that instanceof com.ibm.wala.util.intset.SparseLongSet )
   */
  @Override
  public void copySet(LongSet that) throws UnimplementedError {
    if (that instanceof SparseLongSet) {
      SparseLongSet set = (SparseLongSet) that;
      if (set.elements != null) {
        elements = set.elements.clone();
        size = set.size;
      } else {
        elements = null;
        size = 0;
      }
    } else {
      Assertions.UNREACHABLE();
    }
  }

  @Override
  public void intersectWith(LongSet set) {
    if (set == null) {
      throw new IllegalArgumentException("null set");
    }
    if (set instanceof SparseLongSet) {
      intersectWith((SparseLongSet) set);
    } else {
      int j = 0;
      for (int i = 0; i < size; i++)
        if (set.contains(elements[i]))
          elements[j++] = elements[i];

      size = j;
    }
  }

  public void intersectWith(SparseLongSet set) {
    if (set == null) {
      throw new IllegalArgumentException("null set");
    }
    SparseLongSet that = set;
    if (this.isEmpty()) {
      return;
    } else if (that.isEmpty()) {
      elements = null;
      size = 0;
      return;
    } else if (this.equals(that)) {
      return;
    }

    // some simple optimizations
    if (size == 1) {
      if (that.contains(elements[0])) {
        return;
      } else {
        elements = null;
        size = 0;
        return;
      }
    }
    if (that.size == 1) {
      if (contains(that.elements[0])) {
        if (size > INITIAL_NONEMPTY_SIZE) {
          elements = new long[INITIAL_NONEMPTY_SIZE];
        }
        size = 1;
        elements[0] = that.elements[0];
        return;
      } else {
        elements = null;
        size = 0;
        return;
      }
    }

    long[] ar = this.elements;
    int ai = 0;
    int al = size;
    long[] br = that.elements;
    int bi = 0;
    int bl = that.size;
    long[] cr = null; // allocate on demand
    int ci = 0;

    while (ai < al && bi < bl) {
      long cmp = (ar[ai] - br[bi]);

      // (accept element only on a match)
      if (cmp > 0) { // a greater
        bi++;
      } else if (cmp < 0) { // b greater
        ai++;
      } else {
        if (cr == null) {
          cr = new long[al]; // allocate enough (i.e. too much)
        }
        cr[ci++] = ar[ai];
        ai++;
        bi++;
      }
    }

    // now compact cr to 'just enough'
    size = ci;
    elements = cr;
    return;
  }

  /**
   * Add all elements from another int set.
   * 
   * @return true iff this set changes
   * @throws UnimplementedError if not ( set instanceof com.ibm.wala.util.intset.SparseLongSet )
   */
  @Override
  public boolean addAll(LongSet set) throws UnimplementedError {
    if (set instanceof SparseLongSet) {
      return addAll((SparseLongSet) set);
    } else {
      Assertions.UNREACHABLE();
      return false;
    }
  }

  /**
   * Add all elements from another int set.
   * 
   * @param that
   * @return true iff this set changes
   */
  public boolean addAll(SparseLongSet that) {
    if (that == null) {
      throw new IllegalArgumentException("null that");
    }
    if (this.isEmpty()) {
      copySet(that);
      return !that.isEmpty();
    } else if (that.isEmpty()) {
      return false;
    } else if (this.equals(that)) {
      return false;
    }

    // common-case optimization
    if (that.size == 1) {
      boolean result = add(that.elements[0]);
      return result;
    }

    long[] br = that.elements;
    int bl = that.size();

    return addAll(br, bl);
  }

  private boolean addAll(long[] that, int thatSize) {
    long[] ar = this.elements;
    int ai = 0;
    final int al = size();
    int bi = 0;

    // invariant: assume cr has same value as ar until cr is allocated.
    // we allocate cr lazily when we discover cr != ar.
    long[] cr = null;
    int ci = 0;

    while (ai < al && bi < thatSize) {
      long cmp = (ar[ai] - that[bi]);

      // (always accept element)
      if (cmp > 0) { // a greater
        if (cr == null) {
          cr = new long[al + thatSize];
          System.arraycopy(ar, 0, cr, 0, ci);
        }
        cr[ci++] = that[bi++];
      } else if (cmp < 0) { // b greater
        if (cr != null) {
          cr[ci] = ar[ai];
        }
        ci++;
        ai++;
      } else {
        if (cr != null) {
          cr[ci] = ar[ai]; // (same: use a)
        }
        ci++;
        ai++;
        bi++;
      }
    }

    // append tail if any (at most one of a or b has tail)
    if (ai < al) {
      int tail = al - ai;
      if (cr != null) {
        System.arraycopy(ar, ai, cr, ci, tail);
      }
      ci += tail;
    } else if (bi < thatSize) {
      int tail = thatSize - bi;
      if (cr == null) {
        cr = new long[al + thatSize];
        System.arraycopy(ar, 0, cr, 0, ci);
      }
      System.arraycopy(that, bi, cr, ci, tail);
      ci += tail;
    }

    assert ci > 0;

    elements = (cr == null) ? ar : cr;
    size = ci;
    return (al != size);
  }
}
