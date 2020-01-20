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

public class TestCD3 {

  static void doNothing(Object o) {}

  public static void main(String[] args) {
    Integer I = (Integer) new A().foo();
    int i = I;
    try {
      if (i > 0) {
        System.out.println("X");
        if (i > 1) {
          System.out.println("Y");
        }
      }
    } catch (Throwable e) {
    }
    doNothing(I);
  }
}
