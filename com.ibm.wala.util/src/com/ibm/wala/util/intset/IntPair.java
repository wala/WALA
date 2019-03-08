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
public class IntPair {
  final int x;

  final int y;

  public IntPair(int x, int y) {
    this.x = x;
    this.y = y;
  }

  /** @return Returns the x. */
  public int getX() {
    return x;
  }

  /** @return Returns the y. */
  public int getY() {
    return y;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj.getClass() == this.getClass()) {
      IntPair p = (IntPair) obj;
      return p.getX() == x && p.getY() == y;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 8377 * x + y;
  }

  @Override
  public String toString() {
    return "[" + x + ',' + y + ']';
  }

  public static IntPair make(int x, int y) {
    return new IntPair(x, y);
  }
}
