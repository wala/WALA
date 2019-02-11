/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package demandpa;

import java.util.Arrays;

public class TestArraysCopyOf {

  public static void main(String[] args) {
    Object[] o1 = new Object[1];
    Object[] o2 = new Object[1];
    o1[0] = new A();
    o2[0] = new B();
    Object[] o3 = Arrays.copyOf(o1, 1, Object[].class);
    Arrays.copyOf(o2, 1, Object[].class);
    Object x = o3[0];
    DemandPATestUtil.testThisVar(x);
  }
}
