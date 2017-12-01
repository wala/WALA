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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * simple implementation of IVector
 */
public class SimpleVector<T> implements IVector<T> {
  
  private final static int MAX_SIZE = Integer.MAX_VALUE / 4;

  private final static double GROWTH_FACTOR = 1.5;

  Object[] store = new Object[1];
  
  int maxIndex = -1;

  public SimpleVector() {
  }

  /*
   * @see com.ibm.wala.util.intset.IntVector#get(int)
   */
  @Override
  @SuppressWarnings("unchecked")
  public T get(int x) {
    if (x < 0) {
      throw new IllegalArgumentException("illegal x: " + x);
    }
    if (x < store.length) {
      return (T) store[x];
    } else {
      return null;
    }
  }

  /*
   * @see com.ibm.wala.util.intset.IVector#set(int, int)
   */
  @Override
  public void set(int x, T value) {
    if (x < 0) {
      throw new IllegalArgumentException("illegal x value " + x);
    }
    if (x > MAX_SIZE) {
      throw new IllegalArgumentException("x is too big: " + x);
    }
    maxIndex = Math.max(maxIndex,x);
    if (value == null) {
      if (x >= store.length) {
        return;
      } else {
        store[x] = null;
      }
    } else {
      ensureCapacity(x);
      store[x] = value;
    }
  }

  /**
   * make sure we can store to a particular index
   */
  private void ensureCapacity(int capacity) {
    if (capacity >= store.length) {
      Object[] old = store;
      store = new Object[1 + (int) (GROWTH_FACTOR * capacity)];
      System.arraycopy(old, 0, store, 0, old.length);
    }
  }

  /*
   * @see com.ibm.wala.util.intset.IntVector#reportStats()
   */
  @Override
  public void performVerboseAction() {
    System.err.println(("size:       " + store.length));
    System.err.println(("occupancy:  " + computeOccupancy()));
  }

  /**
   * @return the percentage of entries in delegateStore that are non-null
   */
  private double computeOccupancy() {
    int count = 0;
    for (Object element : store) {
      if (element != null) {
        count++;
      }
    }
    return (double) count / (double) store.length;
  }


  @Override
  @SuppressWarnings("unchecked")
  public Iterator<T> iterator() {
    ArrayList<T> result = new ArrayList<>();
    for (int i =0; i <= maxIndex; i++) {
      result.add((T) store[i]);
    }
    return result.iterator();
  }

  @Override
  public int getMaxIndex() {
    return maxIndex;
  }
}
