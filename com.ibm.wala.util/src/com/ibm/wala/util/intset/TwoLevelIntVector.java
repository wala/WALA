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
import java.util.Vector;

import com.ibm.wala.util.math.Logs;

/**
 * an int vector implementation which delegates to pages of int vectors.
 */
public class TwoLevelIntVector implements IntVector, Serializable {

  private static final int PAGE_SIZE = 4096;

  private static final int LOG_PAGE_SIZE = Logs.log2(PAGE_SIZE);

  int maxIndex = -1;

  /**
   * Array of IntVector: data.get(i) holds data[i*PAGE_SIZE] ... data[(i+1)*PAGESIZE - 1]
   */
  final private Vector<SparseIntVector> data = new Vector<>();

  private final int defaultValue;

  TwoLevelIntVector(int defaultValue) {
    this.defaultValue = defaultValue;
  }

  /*
   * @see com.ibm.wala.util.intset.IntVector#get(int)
   */
  @Override
  public int get(int x) {
    int page = getPageNumber(x);
    if (page >= data.size()) {
      return defaultValue;
    }
    IntVector v = data.get(page);
    if (v == null) {
      return defaultValue;
    }
    int localX = toLocalIndex(x, page);
    return v.get(localX);
  }

  private static int toLocalIndex(int x, int page) {
    return x - getFirstIndexOnPage(page);
  }

  private static int getFirstIndexOnPage(int page) {
    return page << LOG_PAGE_SIZE;
  }

  private static int getPageNumber(int x) {
    return x >> LOG_PAGE_SIZE;
  }

  /*
   * TODO: this can be optimized
   * 
   * @see com.ibm.wala.util.intset.IntVector#set(int, int)
   */
  @Override
  public void set(int x, int value) {
    maxIndex = Math.max(maxIndex, x);
    int page = getPageNumber(x);
    IntVector v = findOrCreatePage(page);
    int localX = toLocalIndex(x, page);
    v.set(localX, value);
  }

  private IntVector findOrCreatePage(int page) {
    if (page >= data.size()) {
      SparseIntVector v = new SparseIntVector(defaultValue);
      data.setSize(page + 1);
      data.add(page, v);
      return v;
    } else {
      SparseIntVector v = data.get(page);
      if (v == null) {
        v = new SparseIntVector(defaultValue);
        data.set(page, v);
      }
      return v;
    }
  }

  /*
   * @see com.ibm.wala.util.debug.VerboseAction#performVerboseAction()
   */
  public void performVerboseAction() {
    System.err.println(("stats of " + getClass()));
    System.err.println(("data: size = " + data.size()));
  }

  @Override
  public int getMaxIndex() {
    return maxIndex;
  }

}
