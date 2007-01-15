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
package com.ibm.wala.util.math;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.Bits;

/**
 *
 * simple utilities with logarithms
 * 
 * @author sfink
 */
public class Logs {

  /**
   * @param x
   * @return true iff x == 2^n for some integral n
   */
  public static boolean isPowerOf2(int x) {
    if (x < 0) {
      return false;
    } else {
      return Bits.populationCount(x) == 1;
    }
  }
  
  /**
   * @param x where x == 2^n for some integral n
   */
  public static int log2(int x) throws IllegalArgumentException {
    if (!isPowerOf2(x)) {
      throw new IllegalArgumentException();
    }
    int test = 1;
    for (int i =0 ; i<31; i++) {
      if (test == x) {
        return i;
      }
      test <<= 1;
    }
    Assertions.UNREACHABLE();
    return -1;
  }
  
}
