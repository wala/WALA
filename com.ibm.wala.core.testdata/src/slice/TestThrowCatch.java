/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package slice;

public class TestThrowCatch {

  static class MyException extends Exception {

    int state;

    MyException(int state) {
      this.state = state;
    }

  }

  public static void callee(int x) throws MyException {
    if (x < 3) {
      MyException exp = new MyException(x);
      throw exp;
    }
  }

  public static void doNothing(int x) {

  }

  public static void main(String args[]) {
    try {
      callee(7);
    } catch (MyException e) {
      int x = e.state;
      doNothing(x);
    }
  }
}
