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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * Test of Constructor.newInstance
 */
public class Reflect23 {
  public static void main(String[] args) throws ClassNotFoundException, SecurityException,
      NoSuchMethodException, IllegalAccessException, InstantiationException,
      IllegalArgumentException, InvocationTargetException {
    Class<?> helperClass = Class.forName("reflection.Helper");
    Object helperObject = helperClass.newInstance();
    Method[] methods = helperClass.getDeclaredMethods();
    for (Method m : methods) {
      if (m.getParameterTypes().length == 1) {
        Class<?> paramType = m.getParameterTypes()[0];
        if (! Modifier.isStatic(m.getModifiers()) && paramType.getName().equals("java.lang.Integer")) {
          Integer i = new Integer(1);
          Object[] initArgs = new Object[]{i};
          m.invoke(helperObject, initArgs);
          break;
        }
      }
    }
  }
}
