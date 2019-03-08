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

    if (array instanceof Object[]) {
      Object[] A = (Object[]) array;
      return A[index];
    } else if (array instanceof int[]) {
      int[] A = (int[]) array;
      return A[index];
    } else if (array instanceof char[]) {
      char[] A = (char[]) array;
      return A[index];
    } else if (array instanceof short[]) {
      short[] A = (short[]) array;
      return A[index];
    } else if (array instanceof long[]) {
      long[] A = (long[]) array;
      return A[index];
    } else if (array instanceof byte[]) {
      byte[] A = (byte[]) array;
      return A[index];
    } else if (array instanceof double[]) {
      double[] A = (double[]) array;
      return A[index];
    } else if (array instanceof boolean[]) {
      boolean[] A = (boolean[]) array;
      return A[index];
    } else if (array instanceof float[]) {
      float[] A = (float[]) array;
      return A[index];
    } else {
      return null;
    }
  }
}
