/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.collections;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.TunedMutableSparseIntSet;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An {@link IVector} implementation designed for low occupancy. Note that get() from this vector is
 * a binary search.
 *
 * <p>This should only be used for small sets ... insertion and deletion are linear in size of set.
 */
public class SparseVector<T> implements IVector<T>, Serializable {

  private static final long serialVersionUID = -6220164684358954867L;

  private static final int DEF_INITIAL_SIZE = 5;

  /** if indices[i] = x, then data[i] == get(x) */
  private MutableSparseIntSet indices = MutableSparseIntSet.makeEmpty();

  private Object[] data;

  public SparseVector() {
    data = new Object[DEF_INITIAL_SIZE];
    indices = MutableSparseIntSet.makeEmpty();
  }

  public SparseVector(int initialSize, float expansion) {
    data = new Object[DEF_INITIAL_SIZE];
    indices = new TunedMutableSparseIntSet(initialSize, expansion);
  }

  /** @see com.ibm.wala.util.intset.IntVector#get(int) */
  @Override
  @SuppressWarnings("unchecked")
  public T get(int x) {
    int index = indices.getIndex(x);
    if (index == -1) {
      return null;
    } else {
      return (T) data[index];
    }
  }

  /**
   * TODO: this can be optimized
   *
   * @see com.ibm.wala.util.intset.IntVector#set(int, int)
   */
  @Override
  public void set(int x, T value) {
    int index = indices.getIndex(x);
    if (index == -1) {
      indices.add(x);
      index = indices.getIndex(x);
      ensureCapacity(indices.size() + 1);
      if (index < (data.length - 1)) {
        System.arraycopy(data, index, data, index + 1, indices.size() - index);
      }
    }
    data[index] = value;
  }

  private void ensureCapacity(int capacity) {
    if (data.length < capacity + 1) {
      data = Arrays.copyOf(data, 1 + (int) (capacity * indices.getExpansionFactor()));
    }
  }

  /** @see com.ibm.wala.util.debug.VerboseAction#performVerboseAction() */
  @Override
  public void performVerboseAction() {
    System.err.println((getClass() + " stats: "));
    System.err.println(("data.length " + data.length));
    System.err.println(("indices.size() " + indices.size()));
  }

  /** @see com.ibm.wala.util.intset.IntSet#intIterator() */
  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {

      int i = 0;

      @Override
      public boolean hasNext() {
        return i < indices.size();
      }

      @Override
      @SuppressWarnings("unchecked")
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return (T) data[i++];
      }

      @Override
      public void remove() {
        // TODO Auto-generated method stub
        Assertions.UNREACHABLE();
      }
    };
  }

  /** @return max i s.t get(i) != null */
  @Override
  public int getMaxIndex() throws IllegalStateException {
    return indices.max();
  }

  public int size() {
    return indices.size();
  }

  public IntIterator iterateIndices() {
    return indices.intIterator();
  }

  /**
   * This iteration _will_ cover all indices even when remove is called while the iterator is
   * active.
   */
  public IntIterator safeIterateIndices() {
    return MutableSparseIntSet.make(indices).intIterator();
  }

  public void clear() {
    data = new Object[DEF_INITIAL_SIZE];
    indices = MutableSparseIntSet.makeEmpty();
  }

  public void remove(int x) {
    int index = indices.getIndex(x);
    if (index == -1) {
      return;
    } else {
      System.arraycopy(data, index + 1, data, index, size() - index - 1);
      indices.remove(x);
    }
  }
}
