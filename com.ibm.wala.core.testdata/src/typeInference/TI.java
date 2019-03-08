/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package typeInference;

public class TI {
  public static void foo() {
    int[] x = new int[0];
    System.out.println(x[0]);
  }

  public void bar(int x) {
    if (x > Integer.MIN_VALUE) {
      Integer.toString(x);
      throw new Error();
    }
  }

  public void inferInt() {
    if (time() < time()) {
      throw new Error();
    }
  }

  private static long time() {
    return System.currentTimeMillis();
  }

  public void useCast(Object o) {
    String s = (String) o;
    System.out.println(s);
  }
}
