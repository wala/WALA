/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package reflection;

public class Helper {
  private final Object a, b;

  public Helper() {
    this.a = new Object();
    this.b = new Object();
    System.out.println("Helper constructor with no parameter invoked");
  }

  public Helper(Integer x) {
    a = b = null;
    x.toString();
    System.out.println(x);
  }

  public Helper(Object a) {
    this.a = a;
    this.b = new Object();
    System.out.println("Helper constructor with one parameter invoked");
  }

  public Helper(Object a, Object b) {
    this.a = a;
    this.b = b;
    System.out.println("Helper constructor with two parameters invoked");
  }

  public void m(Object a, Object b, Object c) {
    System.out.println("m method invoked");
  }

  public void n(Object a, Object b) {
    System.out.println("n method invoked");
  }

  public void o(Object a, Object b) {
    System.out.println("o method invoked");
  }

  public static void s(Object a, Object b) {
    System.out.println("s method invoked");
  }

  public static void t(Integer x) {
    x.toString();
  }

  public void u(Integer x) {
    x.toString();
    System.out.println("u method invoked");
  }
}
