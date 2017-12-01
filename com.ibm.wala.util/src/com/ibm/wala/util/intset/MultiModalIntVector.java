/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation.
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

/**
 * an implementation of {@link IntVector} that uses a mix of backing arrays of type int, char, and byte array, in an attempt to save
 * space for common data structures.
 */
public class MultiModalIntVector implements IntVector {

  private final static float INITIAL_GROWTH_FACTOR = 1.5f;

  private final static float MINIMUM_GROWTH_FACTOR = 1.1f;

  private final static float DIFF_GROWTH_FACTOR = INITIAL_GROWTH_FACTOR - MINIMUM_GROWTH_FACTOR;

  private float CURRENT_GROWTH_RATE = INITIAL_GROWTH_FACTOR;

  private final static int MAX_SIZE = 10000;

  private final static int INITIAL_SIZE = 1;

  int maxIndex = -1;

  private int[] intStore = new int[0];

  private short[] shortStore = new short[0];

  private byte[] byteStore = new byte[0];

  final int defaultValue;

  public MultiModalIntVector(int defaultValue) {
    this.defaultValue = defaultValue;
    init(getInitialSize(), defaultValue);
  }

  private void init(int initialSize, int defaultValue) {
    if (NumberUtility.isByte(defaultValue)) {
      byteStore = new byte[initialSize];
      byteStore[0] = (byte) defaultValue;
    } else if (NumberUtility.isShort(defaultValue)) {
      shortStore = new short[initialSize];
      shortStore[0] = (short) defaultValue;
    } else {
      intStore = new int[initialSize];
      intStore[0] = defaultValue;
    }
  }

  public MultiModalIntVector(int defaultValue, int initialSize) {
    if (initialSize <= 0) {
      throw new IllegalArgumentException("invalid initialSize: " + initialSize);
    }
    this.defaultValue = defaultValue;
    init(initialSize, defaultValue);
  }

  int getInitialSize() {
    return INITIAL_SIZE;
  }

  float getGrowthFactor() {
    return INITIAL_GROWTH_FACTOR;
  }

  /**
   * Will determine a dynamic growth factor that depends on the current size of the array
   * 
   * @param size
   * @return the new growth factor
   */

  float getGrowthFactor(int size) {
    if (CURRENT_GROWTH_RATE >= MINIMUM_GROWTH_FACTOR) {

      float val = (float) (1 / (1 + Math.pow(Math.E, (-1) * ((size / MAX_SIZE) * 12.0 - 6.0))));
      CURRENT_GROWTH_RATE = INITIAL_GROWTH_FACTOR - DIFF_GROWTH_FACTOR * val;
    }
    return CURRENT_GROWTH_RATE;
  }

  /*
   * @see com.ibm.wala.util.intset.IntVector#get(int)
   */
  @Override
  public int get(int x) {
    if (x < 0) {
      throw new IllegalArgumentException("illegal x: " + x);
    }
    int index = x;
    if (index < byteStore.length) {
      return byteStore[index];
    }
    index -= byteStore.length;
    if (index < shortStore.length) {
      return shortStore[index];
    }
    index -= shortStore.length;
    if (index < intStore.length) {
      return intStore[index];
    }
    return defaultValue;
  }

  private int getStoreLength() {
    return shortStore.length + byteStore.length + intStore.length;
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
    maxIndex = Math.max(maxIndex, x); // Find out if the new position is bigger than size of the array
    handleMorph(x, value);
    if (value == defaultValue) {
      int length = getStoreLength();
      if (x >= length) {
        return;
      } else {
        add(value, x);
      }
    } else {
      ensureCapacity(x, value);
      add(value, x);
    }
  }

  private void add(int value, int index) {
    if (byteStore.length > index) {
      byteStore[index] = (byte) value;
    } else {
      index -= byteStore.length;
      if (shortStore.length > index) {
        shortStore[index] = (short) value;
      } else {
        index -= shortStore.length;
        intStore[index] = value;
      }
    }
  }

  private void handleMorph(int index, int value) {
    if (NumberUtility.isShort(value)) {
      if (index < byteStore.length) {
        int newShortSize = byteStore.length - index + shortStore.length;
        short[] newShortStore = new short[newShortSize];
        byte[] newByteStore = new byte[index];
        for (int i = index; i < byteStore.length; i++) {
          newShortStore[i - index] = byteStore[i];
        }
        System.arraycopy(byteStore, 0, newByteStore, 0, index);
        System.arraycopy(shortStore, 0, newShortStore, byteStore.length - index, shortStore.length);
        byteStore = newByteStore;
        shortStore = newShortStore;
      }
    } else if (!NumberUtility.isByte(value)) {
      if (index < byteStore.length) {
        int newShortSize = byteStore.length - index + intStore.length;
        int[] newIntStore = new int[newShortSize];
        for (int i = index; i < byteStore.length; i++) {
          newIntStore[i - index] = byteStore[i];
        }
        byte[] newByteStore = new byte[index];
        System.arraycopy(byteStore, 0, newByteStore, 0, index);
        for (int i = 0; i < shortStore.length; i++) {
          newIntStore[byteStore.length - 1 + i] = shortStore[i];
        }
        System.arraycopy(intStore, 0, newIntStore, byteStore.length + shortStore.length - index, intStore.length);
        intStore = newIntStore;
        byteStore = newByteStore;
        shortStore = new short[0];
      } else {
        int newindex = index - byteStore.length;
        if (newindex < shortStore.length) {
          int newIntSize = shortStore.length - newindex + intStore.length;
          int[] newIntStore = new int[newIntSize];
          for (int i = newindex; i < shortStore.length; i++) {
            newIntStore[i - newindex] = shortStore[i];
          }
          short[] newShortStore = new short[newindex];
          System.arraycopy(shortStore, 0, newShortStore, 0, newindex);
          System.arraycopy(intStore, 0, newIntStore, shortStore.length - newindex, intStore.length);
          intStore = newIntStore;
          shortStore = newShortStore;
        }
      }
    }
  }

  /**
   * make sure we can store to a particular index
   * 
   * @param capacity
   */
  private void ensureCapacity(int capacity, int value) {
    int length = getStoreLength();
    // Value is an int
    if (intStore.length > 0 || (!NumberUtility.isShort(value) && !NumberUtility.isByte(value))) {
      // Need to increase the capacity of the array
      if (capacity >= length) {
        // Current array size
        int[] old = intStore;
        // New array size
        int newSize = 1 + (int) (getGrowthFactor(length) * capacity) - byteStore.length - shortStore.length;
        int[] newData = new int[newSize];
        Arrays.fill(newData, defaultValue);
        System.arraycopy(old, 0, newData, 0, old.length);
        intStore = newData;
      }
    } else if (shortStore.length > 0 || NumberUtility.isShort(value)) {
      if (capacity >= length) {
        short[] old = shortStore;
        int newSize = 1 + (int) (getGrowthFactor(length) * capacity) - byteStore.length;
        short[] newData = new short[newSize];
        Arrays.fill(newData, (short) defaultValue);
        System.arraycopy(old, 0, newData, 0, old.length);
        shortStore = newData;
      }
    } else {
      if (capacity >= length) {
        byte[] old = byteStore;
        int newSize = 1 + (int) (getGrowthFactor(length) * capacity);
        byte[] newData = new byte[newSize];
        Arrays.fill(newData, (byte) defaultValue);
        System.arraycopy(old, 0, newData, 0, old.length);
        byteStore = newData;
      }
    }
  }

  @Override
  public int getMaxIndex() {
    return maxIndex;
  }

  public void print() {
    String str = "";
    for (byte element : byteStore) {
      str += element + ",";
    }
    for (short element : shortStore) {
      str += element + ",";
    }
    for (int element : intStore) {
      str += element + ",";
    }
    System.out.println(str);
  }

}
