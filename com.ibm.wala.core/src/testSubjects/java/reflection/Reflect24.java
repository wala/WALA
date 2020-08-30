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

/**
 * Test of Object.getClass();
 *
 * @author genli
 */
public class Reflect24 {

  public static void main(String[] args) {
    Helper helper = new Helper();
    doNothing(helper.getClass());
  }

  static void doNothing(Class<?> clazz) {}
}
