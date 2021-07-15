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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

public class Reflect27 {

  enum Color {
    RED, GREEN, BLUE;
  }

  /** Test of Enum.values() */
  public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    Class<?> c = Color.class;
    Method m = c.getMethod("values", new Class[] {});
    Object vals = m.invoke(c, new Object[] {});
    Color[] colors = (Color[]) vals;
    for (Color col : colors) {
      System.out.println(col + " at index " + col.ordinal());
    }
  }
}