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
package com.ibm.wala.util.intset;

import java.util.Arrays;

/**
 * an int vector implementation designed for low occupancy. Note that get() from this vector is a
 * binary search.
 *
 * <p>This should only be used for small sets ... insertion and deletion are linear in size of set.
 *
 * <p>In this optimizations, the indices are longs.
 */
public class SparseLongIntVector {

  private static final int INITIAL_SIZE = 5;

  private final double EXPANSION = 1.5;

  /** if indices[i] = x, then data[i] == get(x) */
  private final MutableSparseLongSet indices = new MutableSparseLongSet();

  private int[] data = new int[INITIAL_SIZE];

  private final int defaultValue;

  public SparseLongIntVector(int defaultValue) {
    this.defaultValue = defaultValue;
  }

  /*
   * @see com.ibm.wala.util.intset.IntVector#get(int)
   */
  public int get(long x) {
    int index = indices.getIndex(x);
    if (index == -1) {
      return defaultValue;
    } else {
      return data[index];
    }
  }

  /*
   * TODO: this can be optimized
   *
   * @see com.ibm.wala.util.intset.IntVector#set(int, int)
   */
  public void set(long x, int value) {
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
      data = Arrays.copyOf(data, 1 + (int) (capacity * EXPANSION));
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
}
