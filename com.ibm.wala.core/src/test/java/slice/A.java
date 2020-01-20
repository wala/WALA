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
package slice;

class A {

  Object f;
  Object g;

  Object foo() {
    return new Integer(3);
  }

  public Object getF() {
    return f;
  }

  public void setF(Object f) {
    this.f = f;
  }
}
