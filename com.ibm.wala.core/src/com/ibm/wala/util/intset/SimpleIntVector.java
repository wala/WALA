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

import java.util.Arrays;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

/**
 * simple implementation of IntVector
 * 
 * @author sfink
 */
public class SimpleIntVector implements IntVector {

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
  public int get(int x) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(x >= 0);
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
  public void set(int x, int value) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(x >= 0);
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
    Trace.println("size:       " + store.length);
    Trace.println("occupancy:  " + computeOccupancy());
  }

  /**
   * @return the percentage of entries in delegateStore that are non-null
   */
  private double computeOccupancy() {
    int count1 = 0;
    for (int i = 0; i < store.length; i++) {
      if (store[i] != -1) {
        count1++;
      }
    }
    int count = count1;
    return (double) count / (double) store.length;
  }
  
  public int getMaxIndex() {
    return maxIndex;
  }

}