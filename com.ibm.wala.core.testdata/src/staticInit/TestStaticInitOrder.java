/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package staticInit;

public class TestStaticInitOrder {

  private static class A {

    @SuppressWarnings("unused")
    static int f;
    
    static {
      doNothing();
    }

    private static void doNothing() {
      B.b = 3;
    }
  }

  private static class B {
    @SuppressWarnings("unused")
    static int b;
    
    static {
      foo();
    }
    
    private static void foo() {

    }
  }
  
  private static class C extends B {
    
    @SuppressWarnings("unused")
    static int c = 5;
    
    public static void dostuff() {
      c++;
    }
    
  }
  
  public static void main(String[] args) {
    A.f = 3;
    A.f++;
    C.dostuff();
    B.b = 13;
    B.b = 14;
    C.dostuff();
    A.f++;
  }
}
