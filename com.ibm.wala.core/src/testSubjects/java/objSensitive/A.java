/*
 * Copyright (c) 2002 - 2020 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package objSensitive;

public class A {

  private B b;

  public void set(B b) {
    doSet(b);
  }

  public void doSet(B b) {
    this.b = b;
  }

  public B getB() {
    return this.b;
  }

  public Object foo(Object v) {
    B b = new B();
    return b.bar(v);
  }
}
