/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.model.java.lang.reflect;

/** A synthetic model of java.lang.reflect.Array native methods */
public class Array {

  /** A simple model of object-array copy */
  public static Object get(Object array, int index)
      throws IllegalArgumentException, ArrayIndexOutOfBoundsException {

    if (!array.getClass().isArray()) {
      throw new IllegalArgumentException();
    }

    if (array instanceof Object[] A) {
      return A[index];
    } else if (array instanceof int[] A) {
      return A[index];
    } else if (array instanceof char[] A) {
      return A[index];
    } else if (array instanceof short[] A) {
      return A[index];
    } else if (array instanceof long[] A) {
      return A[index];
    } else if (array instanceof byte[] A) {
      return A[index];
    } else if (array instanceof double[] A) {
      return A[index];
    } else if (array instanceof boolean[] A) {
      return A[index];
    } else if (array instanceof float[] A) {
      return A[index];
    } else {
      return null;
    }
  }
}
