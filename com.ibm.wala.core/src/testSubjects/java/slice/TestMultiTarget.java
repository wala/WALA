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

public class TestMultiTarget {

  static void doNothing(Object o) {}

  /** test a virtual call with multiple targets. slice should include statements assigning to a */
  public static void main(String[] args) {
    A a = null;
    if (args[0] == null) {
      a = new A();
    } else {
      a = new B();
    }
    Object x = a.foo();
    doNothing(x);
  }
}
