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
@SuppressWarnings("javadoc")
public class GetMethodContext {

  public static class A {
    public void foo() {
    }
    public void bar() {
    }
    public void baz() {
    }
  }

  public static class B extends A {
    @Override
    public void foo() {
    }
    @Override
    public void bar() {
    }
    @Override
    public void baz() {
    }
  }

  public static class C extends B {
    @Override
    public void foo() {
    }
    @Override
    public void bar() {
    }
  }

  public static void main(String[] args) throws IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
    Method m;
    A a;
    
    a = new B();
    // As a points to an instance of GetMethodContext$B:
    // Without GetMethodContext, Wala should return GetMethodContext$A#foo() and GetMethodContext$B#foo().
    // With GetMethodContext, Wala should return only GetMethodContext$B#foo().
    m = a.getClass().getMethod("foo");
    m.invoke(a,new Object[]{});

    a = new C();
    // As a points to an instance of GetMethodContext$C:
    // Without GetMethodContext, Wala should return GetMethodContext$C#bar(), GetMethodContext$B#bar() and GetMethodContext$A#bar().
    // With GetMethodContext, Wala should return only GetMethodContext$C#bar().
    m = a.getClass().getDeclaredMethod("bar");
    m.invoke(a,new Object[]{});
    // To summarize:
    //
    // Without GetMethodContext, the call graph must contain
    //  GetMethodContext$B#foo(),
    //  GetMethodContext$A#foo(),
    //  GetMethodContext$C#bar(),
    //  GetMethodContext$B#bar(), and
    //  GetMethodContext$A#bar().
    //
    // With GetMethodContext, the call graph must contain
    //  GetMethodContext$B#foo() and
    //  GetMethodContext$C#bar()
    // and must not contain
    //  GetMethodContext$A#foo(),
    //  GetMethodContext$B#bar(), or
    //  GetMethodContext$A#bar().
    //
    // In either case it must not contain:
    //  GetMethodContext$C#baz(),
    //  GetMethodContext$C#baz(),
    //  GetMethodContext$B#baz(), or
    //  GetMethodContext$A#baz().
  }
}
