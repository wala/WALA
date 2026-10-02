/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
/*
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

/** simple utilities with logarithms */
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
    return Assertions.UNREACHABLE();
  }

  /**
   * Binary log: finds the smallest power k such that 2^k >= n
   *
   * <p>Written as a closed form rather than a loop. The obvious loop, {@code while ((1 << k) < n)},
   * does not terminate once n exceeds 2^30: a shift distance of 32 or more is taken modulo 32, so
   * {@code 1 << 32} is 1 again and the loop cycles forever. The guard covers n &lt;= 1, where n - 1
   * is 0 or negative and would carry the wrong number of leading zeros.
   */
  public static int binaryLogUp(int n) {
    return n <= 1 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(n - 1);
  }

  /**
   * Binary log: finds the smallest power k such that 2^k >= n
   *
   * <p>The same closed form as {@linkplain #binaryLogUp(int) the int overload}, and for the same
   * reason: {@code 1L << k} repeats with a period of 64 once k reaches 64, so a loop written that
   * way never terminates for n > 2^62.
   */
  public static int binaryLogUp(long n) {
    return n <= 1 ? 0 : Long.SIZE - Long.numberOfLeadingZeros(n - 1);
  }
}
