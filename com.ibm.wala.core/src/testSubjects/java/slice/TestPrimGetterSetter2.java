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

public class TestPrimGetterSetter2 {

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
    IntWrapper w1 = new IntWrapper();
    w1.setI(4);
    IntWrapper w2 = new IntWrapper();
    w2.setI(5);
    w2.getI();
    doNothing(w1.getI());
  }
}
