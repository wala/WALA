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
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.ibm.wala.util.debug.Assertions;

/**
 * A sparse ordered, duplicate-free, fully-encapsulated set of longs; not
 * necessary mutable
 * 
 * @author Alan Donovan
 * @author Stephen Fink
 */
public class SparseLongSet implements LongSet {

  private final static int SINGLETON_CACHE_SIZE = 0;

  private final static SparseLongSet[] singletonCache = new SparseLongSet[SINGLETON_CACHE_SIZE];

  static {
    for (int i = 0; i < SINGLETON_CACHE_SIZE; i++) {
      singletonCache[i] = new SparseLongSet(new long[] { i });
    }
  }

  // TODO: I'm not thrilled with exposing these to subclasses, but
  // it seems expedient for now.
  /**
   * The backing store of int arrays
   */
  protected long[] elements;

  /**
   * The number of entries in the backing store that are valid.
   */
  protected int size = 0;

  /*****************************************************************************
   * * * Constructors & Factories *
   ****************************************************************************/

  /**
   * @param size
   */
  protected SparseLongSet(int size) {
    elements = new long[size];
    this.size = size;
  }

  /**
   * Subclasses should use this with extreme care. Do not allow the backing
   * array to escape elsewhere.
   * 
   * @param backingArray
   */
  protected SparseLongSet(long[] backingArray) {
    elements = backingArray;
    this.size = backingArray.length;
  }

  /**
   * Subclasses should use this with extreme care.
   */
  public SparseLongSet() {
    elements = null;
    this.size = 0;
  }

  protected SparseLongSet(SparseLongSet S) {
    cloneState(S);
  }

  /**
   * @param S
   */
  private void cloneState(SparseLongSet S) {
    elements = (long[]) S.elements.clone();
    this.size = S.size;
  }

  /**
   * @param S
   */
  public SparseLongSet(IntSet S) {
    if (S instanceof SparseLongSet) {
      cloneState((SparseLongSet) S);
    } else {
      elements = new long[S.size()];
      size = S.size();
      S.foreach(new IntSetAction() {
        private int index = 0;

        public void act(int i) {
          elements[index++] = i;
        }
      });
    }
  }

  /**
   * Does this set contain value x?
   * 
   * @see com.ibm.wala.util.intset.IntSet#contains(int)
   */
  public final boolean contains(long x) {
    return LongSetUtil.binarySearch(elements, x, 0, size - 1) >= 0;
  }

  /**
   * @param x
   * @return index i s.t. elements[i] == x, or -1 if not found.
   */
  public final int getIndex(long x) {
    return LongSetUtil.binarySearch(elements, x, 0, size - 1);
  }

  /*
   * (non-Javadoc)
   * 
   */
  public final int size() {
    return size;
  }

  /*
   * (non-Javadoc)
   * 
   */
  public final boolean isEmpty() {
    return size == 0;
  }

  public final long elementAt(int idx) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(idx < size);
    }
    return elements[idx];
  }

  private boolean sameValueInternal(SparseLongSet that) {
    if (size != that.size) {
      return false;
    } else {
      for (int i = 0; i < size; i++) {
        if (elements[i] != that.elements[i]) {
          return false;
        }
      }
      return true;
    }
  }

  /*
   * (non-Javadoc)
   * 
   */
  public boolean sameValue(LongSet that) {
    if (that instanceof SparseLongSet) {
      return sameValueInternal((SparseLongSet) that);
    } else {
      Assertions.UNREACHABLE(that.getClass().toString());
      return false;
    }
  }

  /**
   * @return true iff <code>this</code> is a subset of <code>that</code>.
   * 
   * Faster than: <code>this.diff(that) == EMPTY</code>.
   */
  private boolean isSubsetInternal(SparseLongSet that) {

    if (elements == null)
      return true;
    if (that.elements == null)
      return false;
    if (this.equals(that))
      return true;
    if (this.sameValue(that)) {
      return true;
    }

    long[] ar = this.elements;
    int ai = 0;
    int al = size;
    long[] br = that.elements;
    int bi = 0;
    int bl = that.size;

    while (ai < al && bi < bl) {
      long cmp = (ar[ai] - br[bi]);
      // (fail when element only found in 'a')
      if (cmp > 0) { // a greater
        bi++;
      } else if (cmp < 0) { // b greater
        return false;
      } else {
        ai++;
        bi++;
      }
    }
    if (bi == bl && ai < al) {
      // we ran off the end of b without finding an a
      return false;
    }

    return true;
  }

  /**
   * Compute the asymmetric difference of two sets, a \ b.
   */
  public static SparseLongSet diff(SparseLongSet A, SparseLongSet B) {

    if (Assertions.verifyAssertions) {
      Assertions._assert(A != null);
      Assertions._assert(B != null);
    }

    if (A.isEmpty()) {
      return new SparseLongSet(0);
    } else if (B.isEmpty()) {
      return new SparseLongSet(A);
    } else if (A.equals(B)) {
      return new SparseLongSet(0);
    } else if (A.sameValue(B)) {
      return new SparseLongSet(0);
    }

    long[] ar = A.elements;
    int ai = 0;
    int al = A.size;
    long[] br = B.elements;
    int bi = 0;
    int bl = B.size;
    long[] cr = new long[al];
    // allocate enough (i.e. too much)
    int ci = 0;

    while (ai < al && bi < bl) {
      long cmp = (ar[ai] - br[bi]);

      // (accept element when only found in 'a')
      if (cmp > 0) { // a greater
        bi++;
      } else if (cmp < 0) { // b greater
        cr[ci++] = ar[ai];
        ai++;
      } else {
        ai++;
        bi++;
      }
    }

    // append a's tail if any
    if (ai < al) {
      int tail = al - ai;
      System.arraycopy(ar, ai, cr, ci, tail);
      ci += tail;
    }

    // now compact cr to 'just enough'
    ar = new long[ci];
    System.arraycopy(cr, 0, ar, 0, ci); // ar := cr
    return new SparseLongSet(ar);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer sb = new StringBuffer(6 * size);
    sb.append("{ ");
    if (elements != null) {
      for (int ii = 0; ii < size; ii++) {
        sb.append(elements[ii]);
        sb.append(" ");
      }
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Reverse of toString(): "{2,3}" -> [2,3]
   */
  public static long[] parseLongArray(String str) throws NumberFormatException {
    int len = str.length();
    if (str.charAt(0) != '{' || str.charAt(len - 1) != '}')
      throw new NumberFormatException(str);
    str = str.substring(1, len - 1);
    StringTokenizer tok = new StringTokenizer(str, " ,");

    // XXX not very efficient:
    TreeSet<Long> set = new TreeSet<Long>();
    while (tok.hasMoreTokens()) {
      set.add(Long.decode(tok.nextToken()));
    }
    long[] result = new long[set.size()];
    int i = 0;
    for (Iterator<Long> it = set.iterator(); it.hasNext();) {
      Long L = it.next();
      result[i++] = L.longValue();
    }
    return result;
  }


  public static SparseLongSet singleton(int i) {
    if (i >= 0 && i < SINGLETON_CACHE_SIZE) {
      return singletonCache[i];
    } else {
      return new SparseLongSet(new long[] { i });
    }
  }

  public static SparseLongSet pair(long i, long j) {
    if (j > i) {
      return new SparseLongSet(new long[] { i, j });
    } else {
      return new SparseLongSet(new long[] { j, i });
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#intersect(com.ibm.wala.util.intset.IntSet)
   */
  public LongSet intersection(LongSet that) {
    if (that instanceof SparseLongSet) {
      MutableSparseLongSet temp = new MutableSparseLongSet(this);
      temp.intersectWith((SparseLongSet) that);
      return temp;
    } else {
      Assertions.UNREACHABLE("Unexpected: " + that.getClass());
      return null;
    }
  }

  /**
   * @return the largest element in the set
   */
  public final long max() {
    return elements[size - 1];
  }

  /**
   * @param s
   * @param j
   * @return a new sparse int set which adds j to s
   */
  public static SparseLongSet add(SparseLongSet s, int j) {

    SparseLongSet result = new SparseLongSet(s.size + 1);
    int k = 0;
    int m = 0;
    while (k < s.elements.length && s.elements[k] < j) {
      result.elements[k++] = s.elements[m++];
    }
    if (k == s.size) {
      result.elements[k] = j;
    } else {
      if (s.elements[k] == j) {
        result.size--;
      } else {
        result.elements[k++] = j;
      }
      while (k < result.size) {
        result.elements[k++] = s.elements[m++];
      }
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#isSubset(com.ibm.wala.util.intset.IntSet)
   */
  public boolean isSubset(LongSet that) {
    if (that instanceof SparseLongSet) {
      return isSubsetInternal((SparseLongSet) that);
    } else {
      Assertions.UNREACHABLE("Unexpected type " + that.getClass());
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#containsAny(com.ibm.wala.util.intset.IntSet)
   */
  public boolean containsAny(LongSet set) {
    if (set instanceof SparseLongSet) {
      return containsAny((SparseLongSet) set);
    } else {
      for (int i = 0; i < size; i++) {
        if (set.contains(elements[i])) {
          return true;
        }
      }
      return false;
    }
  }

  public boolean containsAny(SparseLongSet set) {
    int i = 0;
    for (int j = 0; j < set.size; j++) {
      long x = set.elements[j];
      while (i < size && elements[i] < x) {
        i++;
      }
      if (i == size) {
        return false;
      } else if (elements[i] == x) {
        return true;
      }
    }
    return false;
  }
}
