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

public class TestThin1 {

  private static void doNothing(Object o) {}

  /** slice should not include any statements relating to base pointers */
  public static void main(String[] args) {
    Object o1 = new Object();
    A a1 = new A();
    A a2 = new A();
    a1.f = a2;
    a2.g = o1;
    A a3 = (A) a1.f;
    Object o3 = a3.g;
    doNothing(o3);
  }
}
