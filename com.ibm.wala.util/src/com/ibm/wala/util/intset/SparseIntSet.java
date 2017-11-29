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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * A sparse ordered, duplicate-free, fully-encapsulated set of integers; not necessary mutable
 */
public class SparseIntSet implements IntSet {

  private static final long serialVersionUID = 2394141733718319022L;

  private final static int SINGLETON_CACHE_SIZE = 5000;

  private final static SparseIntSet[] singletonCache = new SparseIntSet[SINGLETON_CACHE_SIZE];

  static {
    for (int i = 0; i < SINGLETON_CACHE_SIZE; i++) {
      singletonCache[i] = new SparseIntSet(new int[] { i });
    }
  }

  // TODO: I'm not thrilled with exposing these to subclasses, but
  // it seems expedient for now.
  /**
   * The backing store of int arrays
   */
  protected int[] elements;

  /**
   * The number of entries in the backing store that are valid.
   */
  protected int size = 0;

  protected SparseIntSet(int size) {
    elements = new int[size];
    this.size = size;
  }

  /**
   * Subclasses should use this with extreme care. Do not allow the backing array to escape elsewhere.
   */
  protected SparseIntSet(int[] backingArray) {
    if (backingArray == null) {
      throw new IllegalArgumentException("backingArray is null");
    }
    elements = backingArray;
    this.size = backingArray.length;
  }

  /**
   * Subclasses should use this with extreme care.
   */
  public SparseIntSet() {
    elements = null;
    this.size = 0;
  }

  protected SparseIntSet(SparseIntSet S) {
    cloneState(S);
  }


  private void cloneState(SparseIntSet S) {
    if (S.elements != null) {
      elements = S.elements.clone();
    } else {
      elements = null;
    }
    this.size = S.size;
  }

  public SparseIntSet(IntSet S) throws IllegalArgumentException {
    if (S == null) {
      throw new IllegalArgumentException("S == null");
    }
    if (S instanceof SparseIntSet) {
      cloneState((SparseIntSet) S);
    } else {
      elements = new int[S.size()];
      size = S.size();
      S.foreach(new IntSetAction() {
        private int index = 0;

        @Override
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
  @Override
  public final boolean contains(int x) {
    if (elements == null) {
      return false;
    }
    return IntSetUtil.binarySearch(elements, x, 0, size - 1) >= 0;
  }

  /**
   * @return index i s.t. elements[i] == x, or -1 if not found.
   */
  public final int getIndex(int x) {
    if (elements == null) {
      return -1;
    }
    return IntSetUtil.binarySearch(elements, x, 0, size - 1);
  }

  @Override
  public final int size() {
    return size;
  }

  @Override
  public final boolean isEmpty() {
    return size == 0;
  }

  public final int elementAt(int idx) throws NoSuchElementException {
    if (idx < 0) {
      throw new IllegalArgumentException("invalid idx: " + idx);
    }
    if (elements == null || idx >= size) {
      throw new NoSuchElementException("Index: " + idx);
    }
    return elements[idx];
  }

  private boolean sameValueInternal(SparseIntSet that) {
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

  @Override
  public boolean sameValue(IntSet that) throws IllegalArgumentException, UnimplementedError {
    if (that == null) {
      throw new IllegalArgumentException("that == null");
    }
    if (that instanceof SparseIntSet) {
      return sameValueInternal((SparseIntSet) that);
    } else if (that instanceof BimodalMutableIntSet) {
      return that.sameValue(this);
    } else if (that instanceof BitVectorIntSet) {
      return that.sameValue(this);
    } else if (that instanceof MutableSharedBitVectorIntSet) {
      return sameValue(((MutableSharedBitVectorIntSet) that).makeSparseCopy());
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
  private boolean isSubsetInternal(SparseIntSet that) {

    if (elements == null)
      return true;
    if (that.elements == null)
      return false;
    if (this.equals(that))
      return true;
    if (this.sameValue(that)) {
      return true;
    }

    int[] ar = this.elements;
    int ai = 0;
    int al = size;
    int[] br = that.elements;
    int bi = 0;
    int bl = that.size;

    while (ai < al && bi < bl) {
      int cmp = (ar[ai] - br[bi]);
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
  public static SparseIntSet diff(SparseIntSet A, SparseIntSet B) {
    return new SparseIntSet(diffInternal(A, B));
  }

  public static int[] diffInternal(SparseIntSet A, SparseIntSet B) {
    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    if (B == null) {
      throw new IllegalArgumentException("B is null");
    }
    if (A.isEmpty()) {
      return new int[0];
    } else if (B.isEmpty()) {
      int[] newElts = new int[A.size];
      System.arraycopy(A.elements, 0, newElts, 0, A.size);
      return newElts;
    } else if (A.equals(B)) {
      return new int[0];
    } else if (A.sameValue(B)) {
      return new int[0];
    }

    int[] ar = A.elements;
    int ai = 0;
    int al = A.size;
    int[] br = B.elements;
    int bi = 0;
    int bl = B.size;
    int[] cr = new int[al];
    // allocate enough (i.e. too much)
    int ci = 0;

    while (ai < al && bi < bl) {
      int cmp = (ar[ai] - br[bi]);

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
    ar = new int[ci];
    System.arraycopy(cr, 0, ar, 0, ci); // ar := cr
    return ar;
  }

  @Override
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
   * Reverse of toString(): "{2,3}" -&gt; [2,3]
   * @throws IllegalArgumentException if str is null
   */
  public static int[] parseIntArray(String str)  {
    if (str == null) {
      throw new IllegalArgumentException("str is null");
    }
    int len = str.length();
    if (len == 0 || str.charAt(0) != '{' || str.charAt(len - 1) != '}') {
      throw new IllegalArgumentException(str);
    }
    str = str.substring(1, len - 1);
    StringTokenizer tok = new StringTokenizer(str, " ,");

    // XXX not very efficient:
    TreeSet<Integer> set = new TreeSet<>();
    while (tok.hasMoreTokens()) {
      set.add(Integer.decode(tok.nextToken()));
    }
    int[] result = new int[set.size()];
    int i = 0;
    for (Integer I : set) {
      result[i++] = I.intValue();
    }
    return result;
  }

  public static SparseIntSet singleton(int i) {
    if (i >= 0 && i < SINGLETON_CACHE_SIZE) {
      return singletonCache[i];
    } else {
      return new SparseIntSet(new int[] { i });
    }
  }

  public static SparseIntSet pair(int i, int j) {
    if (i == j) {
      return SparseIntSet.singleton(i);
    }
    if (j > i) {
      return new SparseIntSet(new int[] { i, j });
    } else {
      return new SparseIntSet(new int[] { j, i });
    }
  }

  @Override
  public IntSet intersection(IntSet that) {
    if (that == null) {
      throw new IllegalArgumentException("that == null");
    }
    if (that instanceof SparseIntSet) {
      MutableSparseIntSet temp = MutableSparseIntSet.make(this);
      temp.intersectWith((SparseIntSet) that);
      return temp;
    } else if (that instanceof BitVectorIntSet) {
      SparseIntSet s = ((BitVectorIntSet) that).toSparseIntSet();
      MutableSparseIntSet temp = MutableSparseIntSet.make(this);
      temp.intersectWith(s);
      return temp;
    } else if (that instanceof MutableSharedBitVectorIntSet) {
      MutableSparseIntSet temp = MutableSparseIntSet.make(this);
      temp.intersectWith(that);
      return temp;
    } else {
      // this is really slow. optimize as needed.
      MutableSparseIntSet temp = MutableSparseIntSet.makeEmpty();
      for (IntIterator it = intIterator(); it.hasNext();) {
        int x = it.next();
        if (that.contains(x)) {
          temp.add(x);
        }
      }
      return temp;
    }

  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#union(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public IntSet union(IntSet that) {
    MutableSparseIntSet temp = new MutableSparseIntSet();
    temp.addAll(this);
    temp.addAll(that);

    return temp;
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#iterator()
   */
  @Override
  public IntIterator intIterator() {
    return new IntIterator() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return (i < size);
      }

      @Override
      public int next() throws NoSuchElementException {
        if (elements == null) {
          throw new NoSuchElementException();
        }
        return elements[i++];
      }
    };
  }

  /**
   * @return the largest element in the set
   */
  @Override
  public final int max() throws IllegalStateException {
    if (elements == null) {
      throw new IllegalStateException("Illegal to ask max() on an empty int set");
    }
    return (size > 0) ? elements[size - 1] : -1;
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#foreach(com.ibm.wala.util.intset.IntSetAction)
   */
  @Override
  public void foreach(IntSetAction action) {
    if (action == null) {
      throw new IllegalArgumentException("null action");
    }
    for (int i = 0; i < size; i++)
      action.act(elements[i]);
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#foreach(com.ibm.wala.util.intset.IntSetAction)
   */
  @Override
  public void foreachExcluding(IntSet X, IntSetAction action) {
    if (action == null) {
      throw new IllegalArgumentException("null action");
    }
    for (int i = 0; i < size; i++) {
      if (!X.contains(elements[i])) {
        action.act(elements[i]);
      }
    }
  }

  /**
   * @return a new sparse int set which adds j to s
   * @throws IllegalArgumentException if s is null
   */
  public static SparseIntSet add(SparseIntSet s, int j) {

    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    if (s.elements == null) {
      return SparseIntSet.singleton(j);
    }
    SparseIntSet result = new SparseIntSet(s.size + 1);
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
   * @see com.ibm.wala.util.intset.IntSet#isSubset(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public boolean isSubset(IntSet that) {
    if (that == null) {
      throw new IllegalArgumentException("null that");
    }
    if (that instanceof SparseIntSet) {
      return isSubsetInternal((SparseIntSet) that);
    } else if (that instanceof BitVectorIntSet) {
      return isSubsetInternal((BitVectorIntSet) that);
    } else {
      // really slow. optimize as needed.
      for (IntIterator it = intIterator(); it.hasNext();) {
        if (!that.contains(it.next())) {
          return false;
        }
      }
      return true;
    }
  }

  private boolean isSubsetInternal(BitVectorIntSet set) {
    for (int i = 0; i < size; i++) {
      if (!set.contains(elements[i])) {
        return false;
      }
    }
    return true;
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#containsAny(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public boolean containsAny(IntSet set) {
    if (set instanceof SparseIntSet) {
      return containsAny((SparseIntSet) set);
    } else if (set instanceof BimodalMutableIntSet) {
      return set.containsAny(this);
    } else {
      for (int i = 0; i < size; i++) {
        if (set.contains(elements[i])) {
          return true;
        }
      }
      return false;
    }
  }

  public boolean containsAny(SparseIntSet set) throws IllegalArgumentException {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    int i = 0;
    for (int j = 0; j < set.size; j++) {
      int x = set.elements[j];
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

  /**
   * @return contents as an int[]
   */
  public int[] toIntArray() {
    int[] result = new int[size];
    if (size > 0) {
      System.arraycopy(elements, 0, result, 0, size);
    }
    return result;
  }
}
