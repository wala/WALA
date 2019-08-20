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
package multiTypes;

/**
 * Test designed to exercise SubtypesEntrypoint when there is more than one possible type for an
 * argument
 *
 * @author sjfink
 */
public class Foo {

  private static class A {
    public void bar() {}
  }

  private static class B extends A {
    @Override
    public void bar() {}
  }

  public static void foo(A a) {
    a.bar();
  }
}
