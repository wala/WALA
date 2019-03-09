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

public class Slice3 {
  static void doNothing(Object o) {}

  public static void main(String[] args) {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = foo(o1, o2);
    doNothing(o3);
  }

  static Object foo(Object x, Object y) {
    return x;
  }
}
