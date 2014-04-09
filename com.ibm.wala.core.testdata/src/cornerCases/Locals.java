/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cornerCases;

/**
 * @author sfink
 *
 * Simple input test for local variable table
 */
public class Locals {

  public static void foo(String[] a) {
    System.out.println(a);
    Object b = a;
    System.out.println(b);
  }
}
