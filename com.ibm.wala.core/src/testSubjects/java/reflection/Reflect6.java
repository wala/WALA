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
package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Reflect6 {

  public static void main(String[] args)
      throws IllegalAccessException, InstantiationException, ClassNotFoundException,
          IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
          SecurityException {
    Class<?> c = Class.forName("reflection.Reflect6$A");
    Constructor co = c.getDeclaredConstructor(int.class);
    co.setAccessible(true);
    A h = (A) co.newInstance(5);
    System.out.println(h.toString());
  }

  public static class A {
    private A(int i) {}

    @Override
    public String toString() {
      return "Instance of A";
    }
  }
}
