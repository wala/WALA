/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package dynamicCG;

public class MainClass {
  private final Object x;

  private MainClass(Object x) {
    this.x = x;
  }

  private final String printNull() {
    return "*null*";
  }

  private String callSomething(Object x) {
    return "mc:" + (x == null ? printNull() : x.toString());
  }

  private String toStringImpl() {
    try {
      return "mc:" + x.toString();
    } catch (NullPointerException e) {
      return callSomething(x);
    }
  }

  @Override
  public String toString() {
    return toStringImpl();
  }

  public static void main(String[] args) {
    MainClass mc = new MainClass(new ExtraClass("ExtraClass"));
    System.err.println(mc);
    mc = new MainClass(null);
    System.err.println(mc);
    mc = new MainClass(new ExtraClass());
    System.err.println(mc);
  }
}
