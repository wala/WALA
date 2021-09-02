/*
 * Copyright (c) 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Reflect27 {

  /** Test of Enum.values() */
  public static void main(String[] args)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException,
          IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
          SecurityException {
    Class<?> c = Color.class;
    Method m = c.getMethod("values");
    Object vals = m.invoke(c);
    Color[] colors = (Color[]) vals;
    for (Color col : colors) {
      System.out.println(col + " at index " + col.ordinal());
    }
  }

  enum Color {
    RED,
    GREEN,
    BLUE
  }
}
