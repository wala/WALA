/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.cha;

import com.ibm.wala.classLoader.IClassLoader;

/**
 * Statistics about a class hierarchy.
 */
public class ClassHierarchyStats {

  /**
   * Dump stats about the class hierarchy to stdout.
   */
  public static void printStats(IClassHierarchy cha) throws IllegalArgumentException {
    if (cha == null) {
      throw new IllegalArgumentException("cha cannot be null");
    }
    IClassLoader[] loaders = cha.getLoaders();
    for (IClassLoader loader : loaders) {
      System.out.println("loader: " + loader);
      System.out.println("  classes: " + loader.getNumberOfClasses());
      System.out.println("  methods: " + loader.getNumberOfMethods());
    }
  }
}
