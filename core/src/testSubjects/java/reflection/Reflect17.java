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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Test of Method.invoke */
public class Reflect17 {
  public static void main(String[] args)
      throws ClassNotFoundException,
          IllegalArgumentException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException,
          SecurityException,
          NoSuchMethodException {
    Class<?> c = Class.forName("reflection.Helper");
    Method m = c.getDeclaredMethod("t", new Class[] {Integer.class, Integer.class});
    m.invoke(null, new Object[] {null});
  }
}
