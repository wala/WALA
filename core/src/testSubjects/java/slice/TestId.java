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

public class TestId {

  static Object id(Object x) {
    return x;
  }

  static void doNothing(Object o) {}

  /** check for context-sensitive handling of the identity function. o2 should be excluded */
  public static void main(String[] args) {
    Object o1 = new Object(), o2 = new Object();
    Object o3 = id(o1);
    id(o2);
    doNothing(o3);
  }
}
