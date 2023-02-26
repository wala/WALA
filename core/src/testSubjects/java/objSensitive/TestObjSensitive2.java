/*
 * Copyright (c) 2002 - 2020 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package objSensitive;
/** test case for nObjBuilder */
public class TestObjSensitive2 {

  public static void main(String[] args) {
    A a1 = new A();
    Object o1 = new Object(); //  Object/1
    Object result1 = a1.foo(o1);

    A a2 = new A();
    Object o2 = new Object(); //   Object/2
    Object result2 = a2.foo(o2); //   pts(result2) -> {Object/2} , n = 3

    doNothing(result2);
  }

  static void doNothing(Object o) {}
}
