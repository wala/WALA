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
package pi;

class PiNodeCallGraphTestCase {

  interface Whatever {

    void unary1();

    void unary2();

    void binary(Whatever arg);
  }

  static class This implements Whatever {

    @Override
    public void unary1() {
      unary2();
    }

    @Override
    public void unary2() {}

    @Override
    public void binary(Whatever arg) {
      this.unary1();
      arg.unary2();
    }
  }

  static class That implements Whatever {

    @Override
    public void unary1() {}

    @Override
    public void unary2() {
      unary1();
    }

    @Override
    public void binary(Whatever arg) {
      this.unary1();
      arg.unary2();
    }
  }

  public static native boolean choice();

  public static void main(String[] args) {
    Whatever x = new This();
    Whatever y = new That();
    Whatever z = choice() ? x : y;

    if (z instanceof This) x.binary(z);
    else y.binary(z);
    localCast();
  }

  private static void localCast() {
    Whatever y = new That();
    if (y instanceof This) {
      y.binary(y);
    }
  }
}
