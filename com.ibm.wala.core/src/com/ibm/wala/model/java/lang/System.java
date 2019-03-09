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
package com.ibm.wala.model.java.lang;

/** A synthetic model of java.lang.System native methods. */
public class System {

  /** A simple model of object-array copy. This is not completely correct. TODO: fix it. */
  @SuppressWarnings("ManualArrayCopy")
  static void arraycopy(Object src, Object dest) {
    if (src instanceof Object[]) {
      Object[] A = (Object[]) src;
      Object[] B = (Object[]) dest;
      for (int i = 0; i < A.length; i++) B[i] = A[i];
    } else if (src instanceof int[]) {
      int[] A = (int[]) src;
      int[] B = (int[]) dest;
      for (int i = 0; i < A.length; i++) B[i] = A[i];
    } else if (src instanceof char[]) {
      char[] A = (char[]) src;
      char[] B = (char[]) dest;
      for (int i = 0; i < A.length; i++) B[i] = A[i];
    } else if (src instanceof short[]) {
      short[] A = (short[]) src;
      short[] B = (short[]) dest;
      for (int i = 0; i < A.length; i++) B[i] = A[i];
    } else if (src instanceof long[]) {
      long[] A = (long[]) src;
      long[] B = (long[]) dest;
      for (int i = 0; i < A.length; i++) B[i] = A[i];
    } else if (src instanceof byte[]) {
      byte[] A = (byte[]) src;
      byte[] B = (byte[]) dest;
      for (int i = 0; i < A.length; i++) B[i] = A[i];
    } else if (src instanceof double[]) {
      double[] A = (double[]) src;
      double[] B = (double[]) dest;
      for (int i = 0; i < A.length; i++) B[i] = A[i];
    } else if (src instanceof boolean[]) {
      boolean[] A = (boolean[]) src;
      boolean[] B = (boolean[]) dest;
      for (int i = 0; i < A.length; i++) B[i] = A[i];
    } else if (src instanceof float[]) {
      float[] A = (float[]) src;
      float[] B = (float[]) dest;
      for (int i = 0; i < A.length; i++) B[i] = A[i];
    } else {
      throw new ArrayStoreException();
    }
  }
}
