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

/**
 * 
 * Utilities for dealing with LongSets
 * 
 * @author sfink
 */
public class LongSetUtil {

  /**
   * @return index \in [low,high] s.t. data[index] = key, or -1 if not found
   */
  public static int binarySearch(long[] data, long key, int low, int high) throws IllegalArgumentException {
    if (data == null) {
      throw new IllegalArgumentException("null array");
    }
    if (data.length == 0) {
      return -1;
    }
    if (low <= high && (low < 0 || high < 0)) {
      throw new IllegalArgumentException("can't search negative indices");
    }
    if (low <= high) {
      int mid = (low + high) / 2;
      long midValue = data[mid];
      if (midValue == key) {
        return mid;
      } else if (midValue > key) {
        return binarySearch(data, key, low, mid - 1);
      } else {
        return binarySearch(data, key, mid + 1, high);
      }
    } else {
      return -1;
    }
  }
}