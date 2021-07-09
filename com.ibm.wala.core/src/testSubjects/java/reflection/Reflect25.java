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

/** Test of ClassLoader.getSystemClassLoader().loadClass.newInstance */
public class Reflect25 {
  public static void main(String[] args)
      throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
    Class<?> helperClass = ClassLoader.getSystemClassLoader().loadClass("reflection.Helper");
    Class<?> objectClass = ClassLoader.getSystemClassLoader().loadClass("java.lang.Object");
    
    Class<?>[] paramArrayTypes = new Class[] {objectClass, objectClass};
    Method m = helperClass.getMethod("o", paramArrayTypes);
    System.out.println(m.getName());
    Object helperObject = helperClass.getDeclaredConstructor().newInstance();
    Object[] paramArrayObjects = new Object[] {new Object(), new Object()};
    
    m.invoke(helperObject, paramArrayObjects);
  }
}