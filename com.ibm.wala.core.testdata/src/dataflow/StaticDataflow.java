/*
 * Copyright (c) 2010 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package dataflow;

/** test cases for dataflow analysis involving static fields. */
public class StaticDataflow {

  static int f;

  static int g;

  public static void test1() {
    f = 3;
    f = 3;
  }

  public static void test2() {
    f = 4;
    g = 3;
    if (f == 5) {
      g = 2;
    } else {
      g = 7;
    }
  }

  public static void main(String[] args) {
    testInterproc();
  }

  private static void testInterproc() {
    f = 3;
    m();
    g = 4;
    m();
  }

  private static void m() {
    f = 2;
  }
}
