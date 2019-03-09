/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package slice;

public class TestCD4 {

  static void doNothing(int i) {}

  public static void main(String[] args) {
    int i = 3;
    int j = 4;
    int k = foo(i, j);

    doNothing(k);
  }

  static int foo(int i, int j) {
    int k = 0;
    if (i == 3) {
      k = 9;
      if (j != 4) {
        k = k + 1;
      }
      k = 8;
    }
    return k;
  }
}
