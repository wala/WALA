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
package com.ibm.wala.util.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.TunedMutableSparseIntSet;

/**
 * An {@link IVector} implementation designed for low occupancy. Note that get() from this
 * vector is a binary search.
 * 
 * This should only be used for small sets ... insertion and deletion are linear
 * in size of set.
 */
public class SparseVector<T> implements IVector<T> {

  private final static int DEF_INITIAL_SIZE = 5;

  /**
   * if indices[i] = x, then data[i] == get(x)
   */
  private MutableSparseIntSet indices = MutableSparseIntSet.makeEmpty();

  private Object[] data;

  public SparseVector() {
    data = new Object[DEF_INITIAL_SIZE];
    indices = MutableSparseIntSet.makeEmpty();
  }

  /**
   * @param initialSize
   * @param expansion
   */
  public SparseVector(int initialSize, float expansion) {
    data = new Object[DEF_INITIAL_SIZE];
    indices = new TunedMutableSparseIntSet(initialSize, expansion);
  }

  /*
   * @see com.ibm.wala.util.intset.IntVector#get(int)
   */
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
      Object[] old = data;
      data = new Object[1 + (int) (capacity * indices.getExpansionFactor())];
      System.arraycopy(old, 0, data, 0, old.length);
    }
  }

  /*
   * @see com.ibm.wala.util.debug.VerboseAction#performVerboseAction()
   */
  public void performVerboseAction() {
    System.err.println((getClass() + " stats: "));
    System.err.println(("data.length " + data.length));
    System.err.println(("indices.size() " + indices.size()));

  }

  /*
   * @see com.ibm.wala.util.intset.IVector#iterator()
   */
  public Iterator<T> iterator() {
    return new Iterator<T>() {

      int i = 0;

      public boolean hasNext() {
        return i < indices.size();
      }

      @SuppressWarnings("unchecked")
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return (T) data[i++];
      }

      public void remove() {
        // TODO Auto-generated method stub
        Assertions.UNREACHABLE();
      }

    };
  }

  /**
   * @return max i s.t get(i) != null
   */
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
   * This iteration _will_ cover all indices even when remove is called while
   * the iterator is active.
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
