/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
/*******************************************************************************
 * This file includes material derived from code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.util.math;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.Bits;

/**
 * simple utilities with logarithms
 */
public class Logs {

  /**
   * @return true iff x == 2^n for some integer n
   */
  public static boolean isPowerOf2(int x) {
    if (x < 0) {
      return false;
    } else {
      return Bits.populationCount(x) == 1;
    }
  }

  /**
   * @param x where x == 2^n for some integer n
   */
  public static int log2(int x) throws IllegalArgumentException {
    if (!isPowerOf2(x)) {
      throw new IllegalArgumentException();
    }
    int test = 1;
    for (int i = 0; i < 31; i++) {
      if (test == x) {
        return i;
      }
      test <<= 1;
    }
    Assertions.UNREACHABLE();
    return -1;
  }

  /** Binary log: finds the smallest power k such that 2^k &gt;= n */
  public static int binaryLogUp(int n) {
    int k = 0;
    while ((1 << k) < n) {
      k++;
    }
    return k;
  }

  /** Binary log: finds the smallest power k such that 2^k &gt;= n */
  public static int binaryLogUp(long n) {
    int k = 0;
    while ((1 << k) < n) {
      k++;
    }
    return k;
  }

}
