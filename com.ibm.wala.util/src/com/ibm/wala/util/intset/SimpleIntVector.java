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

import java.io.Serializable;
import java.util.Arrays;

/**
 * simple implementation of IntVector
 */
public class SimpleIntVector implements IntVector, Serializable {
  
  private static final long serialVersionUID = -7909547846468543777L;

  private final static int MAX_SIZE = Integer.MAX_VALUE / 4;

  private final static float GROWTH_FACTOR = 1.5f;

  private final static int INITIAL_SIZE = 1;
  
  int maxIndex = -1;

  int[] store;

  final int defaultValue;
  
  public SimpleIntVector(int defaultValue) {
    this.defaultValue = defaultValue;
    store = new int[getInitialSize()];
    store[0] = defaultValue;
  }

  public SimpleIntVector(int defaultValue, int initialSize) {
    if (initialSize <= 0) {
      throw new IllegalArgumentException("Illegal initialSize: " + initialSize);
    }
    this.defaultValue = defaultValue;
    store = new int[initialSize];
    store[0] = defaultValue;
  }

  int getInitialSize() {
    return INITIAL_SIZE;
  }

  float getGrowthFactor() {
    return GROWTH_FACTOR;
  }

  /*
   * @see com.ibm.wala.util.intset.IntVector#get(int)
   */
  @Override
  public int get(int x) {
    if (x < 0) {
      throw new IllegalArgumentException("illegal x: " + x);
    }
    if (x < store.length) {
      return store[x];
    } else {
      return defaultValue;
    }
  }

  /*
   * @see com.ibm.wala.util.intset.IntVector#set(int, int)
   */
  @Override
  public void set(int x, int value) {
    if (x < 0) {
      throw new IllegalArgumentException("illegal x: " + x);
    }
    if (x > MAX_SIZE) {
      throw new IllegalArgumentException("x is too big: " + x);
    }
    maxIndex = Math.max(maxIndex,x);
    if (value == defaultValue) {
      if (x >= store.length) {
        return;
      } else {
        store[x] = value;
      }
    } else {
      ensureCapacity(x);
      store[x] = value;
    }
  }

  /**
   * make sure we can store to a particular index
   * 
   * @param capacity
   */
  private void ensureCapacity(int capacity) {
    if (capacity >= store.length) {
      int[] old = store;
      store = new int[1 + (int) (getGrowthFactor() * capacity)];
      Arrays.fill(store, defaultValue);
      System.arraycopy(old, 0, store, 0, old.length);
    }
  }

  /*
   * @see com.ibm.wala.util.intset.IntVector#reportStats()
   */
  public void performVerboseAction() {
    System.err.println(("size:       " + store.length));
    System.err.println(("occupancy:  " + computeOccupancy()));
  }

  /**
   * @return the percentage of entries in delegateStore that are non-null
   */
  private double computeOccupancy() {
    int count1 = 0;
    for (int element : store) {
      if (element != -1) {
        count1++;
      }
    }
    int count = count1;
    return (double) count / (double) store.length;
  }
  
  @Override
  public int getMaxIndex() {
    return maxIndex;
  }

}
