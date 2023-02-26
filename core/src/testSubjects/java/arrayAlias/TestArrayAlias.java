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
package arrayAlias;

public class TestArrayAlias {

  public static void main(String[] args) {
    Object[] o = new Integer[10];
    testMayAlias1(o, (Integer[]) o);
    Object[] o2 = new Object[20];
    testMayAlias2(o2, (Object[][]) o2);
    testMayAlias3((Integer[]) o, (String[]) o);
  }

  private static void testMayAlias1(Object[] o, Integer[] o2) {}

  private static void testMayAlias2(Object[] o, Object[][] o2) {}

  private static void testMayAlias3(Integer[] o, String[] o2) {}
}
