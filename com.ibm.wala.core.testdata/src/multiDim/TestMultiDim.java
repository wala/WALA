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
package multiDim;

public class TestMultiDim {

  static void doNothing(Object o) {}
 
  public static void main(String[] args) {
    Object[][] multi = new Object[10][];
    multi[0] = new Object[10];
    testMulti(multi);
  }
 
  static void testMulti(Object[][] multi) {
    Object[] t = multi[0];
    doNothing(t);
  }

  static void testNewMultiArray() {
    String[][][] x = new String[3][4][];
    doNothing(x);
  }
}
