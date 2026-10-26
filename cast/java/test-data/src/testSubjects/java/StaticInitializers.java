/*
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
public class StaticInitializers {

  static class X {
    int x;
    int y;

    int sum() {
      return x+y;
    }

    int diff() {
      //noinspection UnnecessaryUnaryMinus
      return x+-y;
    }
  }

  private static X x = new X();
  private static X y;

  static {
    y = new X();
  }

  private int sum() {
    return x.sum() * y.diff();
  }

  public static void main(String[] args) {
    StaticInitializers SI = new StaticInitializers();
    SI.sum();
  }
}
