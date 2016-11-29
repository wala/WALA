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
package com.ibm.wala.util.graph;

import com.ibm.wala.util.intset.SimpleIntVector;

/**
 * We represent a path in a numbered graph as a vector of integers &lt;i_1, ...,
 * i_n&gt; where node i_1 is the src and node i_n is the sink
 */
public class Path extends SimpleIntVector {

  final int size;

  private Path(int defaultValue, int size) {
    super(defaultValue, size);
    this.size = size;
  }

  public static Path make(int value) {
    return new Path(value, 1);
  }

  public static Path prepend(int x, Path p) {
    if (p == null) {
      throw new IllegalArgumentException("null p");
    }
    Path result = new Path(0, p.size + 1);
    result.set(0, x);
    for (int i = 0; i < p.size; i++) {
      result.set(i + 1, p.get(i));
    }
    return result;
  }

  @Override
  public int hashCode() {
    int result = 7;
    for (int i = 0; i < size; i++) {
      result += 31 * (get(i) + 1);
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Path) {
      Path other = (Path) obj;
      if (size == other.size) {
        for (int i = 0; i < size; i++) {
          if (get(i) != other.get(i)) {
            return false;
          }
        }
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  public int size() {
    return size;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("[");
    for (int i = 0; i < size; i++) {
      result.append(get(i));
      if (i < size -1) {
        result.append(",");
      }
    }
    result.append("]");
    return result.toString();
  }

}