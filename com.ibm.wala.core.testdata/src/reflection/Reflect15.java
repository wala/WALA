/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Test of Class.getConstructors().
 */
public class Reflect15 {
  @SuppressWarnings("null")
  public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    Class<?> c = Class.forName("reflection.Helper");
    Constructor<?>[] ctors = c.getConstructors();
    Helper h = null;
    for (Constructor<?> ctor : ctors) {
      if (ctor.getParameterTypes().length == 2) {
        h = (Helper) ctor.newInstance(new Object[] {new Object(), new Object()});
      }
    }
    h.n(new Object(), new Object());
  }
}
