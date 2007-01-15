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
 * 
 * An implementation of mutable int set that delegates to either a
 * MutableSparseIntSet of a BitVectorIntSet
 * 
 * @author sfink
 */
public class BimodalMutableIntSet implements MutableIntSet {

  MutableIntSet impl;

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#copySet(com.ibm.wala.util.intset.IntSet)
   */
  public void copySet(IntSet set) {
    if (set instanceof BimodalMutableIntSet) {
      impl = IntSetUtil.makeMutableCopy(((BimodalMutableIntSet) set).impl);
    } else if (sameRepresentation(impl, set)) {
      // V and other.V use the same representation. update in place.
      impl.copySet(set);
    } else if (set instanceof BitVectorIntSet || set instanceof SparseIntSet) {
      // other.V has a different representation. make a new copy
      impl = IntSetUtil.makeMutableCopy(set);
    } else if (set instanceof MutableSharedBitVectorIntSet) {
      impl = IntSetUtil.makeMutableCopy(((MutableSharedBitVectorIntSet) set).makeSparseCopy());
    } else {
      Assertions.UNREACHABLE("Unexpected type " + set.getClass());
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(impl instanceof BitVectorIntSet || impl instanceof MutableSparseIntSet);
    }
  }

  /**
   * @param V
   * @param W
   * @return true iff we would like to use the same representation for V as we
   *         do for W
   */
  private boolean sameRepresentation(IntSet V, IntSet W) {
    // for now we assume that we always want to use the same representation for
    // V as
    // for W.
    return (V.getClass().equals(W.getClass()));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#addAll(com.ibm.wala.util.intset.IntSet)
   */
  public boolean addAll(IntSet set) {
    if (set instanceof BitVectorIntSet && !(impl instanceof BitVectorIntSet)) {
      // change the representation before performing the operation
      impl = new BitVectorIntSet(impl);
    }
    boolean result = impl.addAll(set);
    if (result) {
      maybeChangeRepresentation();
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#addAll(com.ibm.wala.util.intset.IntSet)
   */
  public boolean addAllInIntersection(IntSet other, IntSet filter) {
    if (other instanceof BitVectorIntSet && !(impl instanceof BitVectorIntSet)) {
      // change the representation before performing the operation
      impl = new BitVectorIntSet(impl);
    }
    boolean result = impl.addAllInIntersection(other, filter);
    if (result) {
      maybeChangeRepresentation();
    }
    return result;
  }

  /**
   * If appropriate, change the representation of V.
   * 
   * For now, this method will change a MutableSparseIntSet to a BitVector if it
   * saves space.
   * 
   * TODO: add more variants.
   */
  private void maybeChangeRepresentation() {
    if (Assertions.verifyAssertions) {
      Assertions._assert(impl instanceof BitVectorIntSet || impl instanceof MutableSparseIntSet);
    }
    if (impl == null) {
      return;
    }

    int sparseSize = impl.size();
    if (sparseSize <= 2) {
      // don't bother
      return;
    }
    int bvSize = BitVector.subscript(impl.max()) + 1;
    // Trace.println("S B " + sparseSize + " " + bvSize + " " + impl.max() + " "
    // + impl);
    if (sparseSize > bvSize) {
      if (!(impl instanceof BitVectorIntSet)) {
        impl = new BitVectorIntSet(impl);
      }
    } else {
      if (!(impl instanceof MutableSparseIntSet)) {
        impl = new MutableSparseIntSet(impl);
      }
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(impl instanceof BitVectorIntSet || impl instanceof MutableSparseIntSet);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#add(int)
   */
  public boolean add(int i) {
    boolean result = impl.add(i);
    if (result) {
      maybeChangeRepresentation();
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#remove(int)
   */
  public boolean remove(int i) {
    boolean result = impl.remove(i);
    maybeChangeRepresentation();
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#intersectWith(com.ibm.wala.util.intset.IntSet)
   */
  public void intersectWith(IntSet set) {
    if (set instanceof BimodalMutableIntSet) {
      BimodalMutableIntSet that = (BimodalMutableIntSet) set;
      impl.intersectWith(that.impl);
    } else {
      Assertions.UNREACHABLE();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#contains(int)
   */
  public boolean contains(int i) {
    return impl.contains(i);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#intersection(com.ibm.wala.util.intset.IntSet)
   */
  public IntSet intersection(IntSet that) {
    if (that instanceof BimodalMutableIntSet) {
      BimodalMutableIntSet b = (BimodalMutableIntSet) that;
      return impl.intersection(b.impl);
    } else if (that instanceof BitVectorIntSet) {
      return impl.intersection(that);
    } else {
      Assertions.UNREACHABLE("Unexpected: " + that);
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#isEmpty()
   */
  public boolean isEmpty() {
    return impl.isEmpty();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#size()
   */
  public int size() {
    return impl.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#iterator()
   */
  public IntIterator intIterator() {
    return impl.intIterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#foreach(com.ibm.wala.util.intset.IntSetAction)
   */
  public void foreach(IntSetAction action) {
    impl.foreach(action);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#foreachExcluding(com.ibm.wala.util.intset.IntSet,
   *      com.ibm.wala.util.intset.IntSetAction)
   */
  public void foreachExcluding(IntSet X, IntSetAction action) {
    impl.foreachExcluding(X, action);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#max()
   */
  public int max() throws IllegalStateException{
    return impl.max();
  }

  public static BimodalMutableIntSet makeCopy(IntSet B) {
    if (B instanceof BimodalMutableIntSet) {
      BimodalMutableIntSet that = (BimodalMutableIntSet) B;
      BimodalMutableIntSet result = new BimodalMutableIntSet();
      result.impl = IntSetUtil.makeMutableCopy(that.impl);
      return result;
    } else if (B instanceof MutableSharedBitVectorIntSet) {
      BimodalMutableIntSet result = new BimodalMutableIntSet();
      MutableSharedBitVectorIntSet s = (MutableSharedBitVectorIntSet) B;
      result.impl = IntSetUtil.makeMutableCopy(s.makeSparseCopy());
      if (Assertions.verifyAssertions) {
        Assertions._assert(result.impl instanceof BitVectorIntSet || result.impl instanceof MutableSparseIntSet);
      }
      return result;
    } else {
      BimodalMutableIntSet result = new BimodalMutableIntSet();
      result.impl = IntSetUtil.makeMutableCopy(B);
      if (Assertions.verifyAssertions) {
        Assertions._assert(result.impl instanceof BitVectorIntSet || result.impl instanceof MutableSparseIntSet);
      }
      return result;
    }

  }

  public BimodalMutableIntSet() {
    impl = new MutableSparseIntSet();
  }

  public BimodalMutableIntSet(int initialSize, float expansionFactor) {
    impl = new TunedMutableSparseIntSet(initialSize, expansionFactor);
  }

  /**
   * @param x
   */
  public BimodalMutableIntSet(BimodalMutableIntSet x) {
    impl = IntSetUtil.makeMutableCopy(x.impl);
    if (Assertions.verifyAssertions) {
      Assertions._assert(impl instanceof BitVectorIntSet || impl instanceof MutableSparseIntSet);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#sameValue(com.ibm.wala.util.intset.IntSet)
   */
  public boolean sameValue(IntSet that) {
    return impl.sameValue(that);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#isSubset(com.ibm.wala.util.intset.SparseIntSet)
   */
  public boolean isSubset(IntSet that) {
    if (that instanceof BimodalMutableIntSet) {
      BimodalMutableIntSet b = (BimodalMutableIntSet) that;
      return impl.isSubset(b.impl);
    } else {
      Assertions.UNREACHABLE();
      return false;
    }
  }

  /**
   * use with care
   * 
   */
  public IntSet getBackingStore() {
    return impl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return impl.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#containsAny(com.ibm.wala.util.intset.IntSet)
   */
  public boolean containsAny(IntSet that) {
    if (that instanceof BimodalMutableIntSet) {
      BimodalMutableIntSet b = (BimodalMutableIntSet) that;
      return impl.containsAny(b.impl);
    } else if (that instanceof SparseIntSet) {
      return impl.containsAny(that);
    } else if (that instanceof BitVectorIntSet) {
      return impl.containsAny(that);
    } else {
      Assertions.UNREACHABLE("unsupported " + that.getClass());
      return false;
    }
  }

  /**
   * TODO: optimize ME!
   */
  public boolean removeAll(IntSet that) {
    boolean result = false;
    for (IntIterator it = that.intIterator(); it.hasNext();) {
      result |= remove(it.next());
    }
    return result;

  }

  /**
   * TODO: optimize ME!
   */
  public boolean containsAll(BimodalMutableIntSet that) {
    for (IntIterator it = that.intIterator(); it.hasNext();) {
      if (!contains(it.next())) {
        return false;
      }
    }
    return true;
  }
}