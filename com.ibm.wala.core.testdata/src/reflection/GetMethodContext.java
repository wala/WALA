/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A test targeting the {@link com.ibm.wala.analysis.reflection.GetMethodContext}.
 * @author Michael Heilmann
 */
public class GetMethodContext {

  public static class A {
    public void bar() {
    }
    public void foo() {
    }
    public void baz() {
    }
  };

  public static class B extends A {
    @Override
    public void bar() {
    }
    @Override
    public void foo() {
    }
    @Override
    public void baz() {
    }
  };

  public static class C extends B {
    @Override
    public void foo() {
    }
    @Override
    public void baz() {
    }
  };

  public static void main(String[] args) throws IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
    Method m;
    A a;
    a = new B();
    // Wala should return only GetMethodContext$A#foo() and GetMethodContext$B#foo().
    // TODO
    // Wala should return only GetMethodContext$B#foo().
    m = a.getClass().getMethod("foo");
    m.invoke(new Object[]{});
    // Wala should return only GetMethodContext$B#bar().
    m = a.getClass().getDeclaredMethod("bar");
    m.invoke(new Object[]{});
    a = new C();
    // Wala should return only GetMethodContext$C#baz().
    m = a.getClass().getDeclaredMethod("baz");
    m.invoke(new Object[]{});
    // To summarize:
    // 1 x GetMethodContext$A#foo()
    // 1 x GetMethodContext$B#foo()
    // 1 x GetMethodContext$B#bar()
    // 1 x GetMethodContext$C#baz()
  }
}
