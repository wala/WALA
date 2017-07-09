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

import java.io.FilePermission;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 
 * @author pistoia
 * 
 */
public class Reflect7 {
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException,
      IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Class<FilePermission> c = (Class<FilePermission>) Class.forName("java.io.FilePermission");
    Class<?>[] paramTypes = new Class[] { "".getClass(), "".getClass() };
    Constructor<FilePermission> constr = c.getConstructor(paramTypes);
    Object[] params = new String[] { "log.txt", "read" };
    FilePermission fp = constr.newInstance(params);
    fp.toString();
  }
}
