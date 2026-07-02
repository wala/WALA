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
package com.ibm.wala.util.intset;

/** A pair of ints. Note that an IntPair has value semantics. */
public record IntPair(int x, int y) {

  /**
   * @deprecated Use {@link #x()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public int getX() {
    return x();
  }

  /**
   * @deprecated Use {@link #y()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public int getY() {
    return y();
  }

  @Override
  public String toString() {
    return "[" + x + ',' + y + ']';
  }

  public static IntPair make(int x, int y) {
    return new IntPair(x, y);
  }
}
