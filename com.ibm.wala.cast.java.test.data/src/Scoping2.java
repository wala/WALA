/******************************************************************************
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
public class Scoping2 {
  public static void main(String[] args) {
    Scoping2 s2 = new Scoping2();
    {
      final int x = 5;
      System.out.println(x);
      (new Object() {
        public void foo() {
          System.out.println("x = " + x);
        }
      }).foo();
    }
    {
      double x = 3.14;
      System.out.println(x);
    }
  }
}
