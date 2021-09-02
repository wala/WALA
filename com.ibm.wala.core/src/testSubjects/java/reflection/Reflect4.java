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

import java.io.FilePermission;
import java.lang.reflect.InvocationTargetException;

public class Reflect4 {

  public static void main(String[] args)
      throws IllegalAccessException, InstantiationException, ClassNotFoundException,
          IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
          SecurityException {
    Class<?> c = Class.forName("java.io.FilePermission");
    FilePermission h =
        (FilePermission)
            c.getDeclaredConstructor("".getClass(), "".getClass()).newInstance("log.txt", "read");
    System.out.println(h.toString());
  }
}
