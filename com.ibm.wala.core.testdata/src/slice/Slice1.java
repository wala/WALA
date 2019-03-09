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

public class Slice1 {
  public static void main(String[] args) {
    int x = foo(1);
    int y = bar(2);
    System.out.println(x + y);
  }

  static int foo(int x) {
    return x + 2;
  }

  static int bar(int x) {
    return x + 3;
  }
}
