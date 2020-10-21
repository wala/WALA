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

public class TestPrimGetterSetter {

  static class IntWrapper {
    int i;

    int getI() {
      return i;
    }

    void setI(int i) {
      this.i = i;
    }
  }

  public static void doNothing(int i) {}

  public static void main(String[] args) {
    IntWrapper w = new IntWrapper();
    test(w);
  }

  public static void test(IntWrapper w) {
    int x = 3;
    w.setI(x);
    int y = w.getI();
    doNothing(y); // slice on y
  }
}
