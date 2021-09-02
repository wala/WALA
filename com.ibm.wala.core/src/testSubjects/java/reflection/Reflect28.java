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
import java.util.ServiceLoader;

public class Reflect28 {

  /** Test of ServiceLoader */
  public static void main(String[] args)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException,
          IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
          SecurityException {

    ServiceLoader<HelperInterface> loader = ServiceLoader.load(HelperInterface.class);

    for (HelperInterface service0 : loader) { // there should be two implementors

      System.out.println(service0.display());

      HelperInterface s0 = service0.getClass().getDeclaredConstructor().newInstance();
      System.out.println(s0.display());
    }
  }
}
