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

public class TestGlobal {

  static Object global1;

  static Object global2;

  static void copyGlobals() {
    global2 = global1;
  }

  static void doNothing(Object o) {}

  /** make sure global variables are being properly handled */
  public static void main(String[] args) {
    global1 = new Object();
    copyGlobals();
    Object x = global2;
    doNothing(x);
  }
}
