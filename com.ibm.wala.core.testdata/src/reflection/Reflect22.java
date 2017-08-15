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
 * Test of Constructor.newInstance
 */
public class Reflect22 {
  public static void main(String[] args) throws ClassNotFoundException, SecurityException,
      NoSuchMethodException, IllegalAccessException, InstantiationException,
      IllegalArgumentException, InvocationTargetException {
    Class<?> helperClass = Class.forName("reflection.Helper");
    Constructor<?>[] constrs = helperClass.getDeclaredConstructors();
    for (Constructor<?> constr : constrs) {
      if (constr.getParameterTypes().length == 1) {
        Class<?> paramType = constr.getParameterTypes()[0];
        if (paramType.getName().equals("java.lang.Integer")) {
          Integer i = new Integer(1);
          Object[] initArgs = new Object[]{i};
          constr.newInstance(initArgs);
          break;
        }
      }
    }
  }
}
