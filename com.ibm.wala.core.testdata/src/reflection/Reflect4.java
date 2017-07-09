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

public class Reflect4 {

  public static void main(String[] args) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
    Class<?> c = Class.forName("java.io.FilePermission");
    FilePermission h = (FilePermission) c.newInstance();
    System.out.println(h.toString());
  }
}
