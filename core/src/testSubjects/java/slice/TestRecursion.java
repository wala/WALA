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

public class TestRecursion {

  static Object find(A a, Object o) {
    if (o == null) {
      return a;
    } else {
      return find((A) a.f, o);
    }
  }

  static void doNothing(Object o) {}

  /** test of recursion. Everything for a1, a2, and a3 should be included */
  public static void main(String[] args) {
    A a1 = new A(), a2 = new A(), a3 = new A();
    a1.f = a2;
    a2.f = a3;
    Object x = find(a1, args[0]);
    doNothing(x);
  }
}
