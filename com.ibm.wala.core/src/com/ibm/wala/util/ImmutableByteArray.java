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
package com.ibm.wala.util;

import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * A read-only byte array.
 * 
 * @author sfink
 */
public final class ImmutableByteArray {

  // allow "friends" in this package to access
  final byte[] b;

  public ImmutableByteArray(byte[] b) {
    this.b = b;
  }

  public ImmutableByteArray(byte[] b, int start, int length) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    this.b = new byte[length];
    if (Assertions.verifyAssertions) {
      Assertions._assert(b.length >= start + length, "illegal");
    }
    System.arraycopy(b, start, this.b, 0, length);
  }

  public int length() {
    return b.length;
  }

  public byte get(int i) throws IllegalArgumentException {
    if (i >= b.length) {
      throw new IllegalArgumentException("index out of bounds " + b.length + " " + i);
    }
    return b[i];
  }

  public byte[] substring(int i, int length) {
    byte[] result = new byte[length];
    System.arraycopy(b, i, result, 0, length);
    return result;
  }

  public static ImmutableByteArray concat(byte b, ImmutableByteArray b1) {
    if (b1 == null) {
      throw new IllegalArgumentException("b1 is null");
    }
    byte[] arr = new byte[b1.length() + 1];
    arr[0] = b;
    System.arraycopy(b1.b, 0, arr, 1, b1.b.length);
    return new ImmutableByteArray(arr);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return new String(b);
  }

  public static ImmutableByteArray make(String s) {
    return new ImmutableByteArray(UTF8Convert.toUTF8(s));
  }

}