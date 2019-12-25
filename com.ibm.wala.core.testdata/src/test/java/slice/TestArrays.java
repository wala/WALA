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

public class TestArrays {

  static void doNothing(Object o) {}
  /** slice should include statements involving arr2 and i, exclude statements with arr1 and j */
  public static void main(String[] args) {
    Object[] arr1 = new Object[10], arr2 = new Object[10];
    int i = 3;
    int j = 4;
    arr2[i] = new Object();
    arr1[j] = new Object();
    Object x = arr2[i];
    doNothing(x);
  }
}
