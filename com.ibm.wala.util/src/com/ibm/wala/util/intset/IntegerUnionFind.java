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
package com.ibm.wala.util.intset;


/**
 * An implementation of Tarjan's union-find, using path compression and balancing, for non-negative integers
 */
public class IntegerUnionFind {
  
  private final static int MAX_VALUE = Integer.MAX_VALUE / 4;

  final private static int DEFAULT_SIZE = 100;
  /**
   * 
   * parent[i+1] =
   * <ul>
   * &lt;li&gt;j &gt; 0 if i is in the same equiv class as j
   * <li>j &lt;= 0 if i is the representative of a class of size -(j)+1
   * </ul>
   * 
   * we initialize parent[j] = 0, so each element is a class of size 1
   */
  int[] parent;

  public IntegerUnionFind() {
    this(DEFAULT_SIZE);
  }

  /**
   * @param size initial size of the tables
   */
  public IntegerUnionFind(int size) {
    if (size  < 0 || size > MAX_VALUE) {
      throw new IllegalArgumentException("illegal size: " + size);
    }
    parent = new int[size + 1];
  }

  /**
   * union the equiv classes of x and y
   */
  public void union(int x, int y) {
    if (x < 0) {
      throw new IllegalArgumentException("invalid x : " + x);
    }
    if (y < 0) {
      throw new IllegalArgumentException("invalid y: " + y);
    }
    if (x > MAX_VALUE) {
      throw new IllegalArgumentException("x is too big: " + x);
    }
    if (y > MAX_VALUE) {
      throw new IllegalArgumentException("y is too big: " + y);
    }
    if (x >= size() || y >= size()) {
      grow(2 * Math.max(x, y));
    }
    // shift by one to support sets including 0
    x += 1;
    y += 1;
    x = findInternal(x);
    y = findInternal(y);
    if (x != y) {
      // glue smaller tree onto larger tree (balancing)
      if (parent[x] < parent[y]) {
        // x's class has more elements
        parent[x] += parent[y] - 1;
        parent[y] = x;
      } else {
        // y's class has more elements (or the same number)
        parent[y] += parent[x] - 1;
        parent[x] = y;
      }
    }
  }

  private void grow(int size) {
    int[] old = parent;
    parent = new int[size + 1];
    System.arraycopy(old, 0, parent, 0, old.length);
  }

  /**
   * @param x
   * @return representative of x's equivalence class
   */
  public int find(int x) {
    if (x < 0) {
      throw new IllegalArgumentException("illegal x " + x);
    }
    if (x >= size()) {
      return x;
    }

    // shift by one to support sets including 0
    return findInternal(x + 1) - 1;
  }

  /**
   * @param x
   * @return representative of x's equivalence class
   */
  private int findInternal(int x) {
    int root = x;
    while (parent[root] > 0) {
      root = parent[root];
    }
    // path compression
    while (parent[x] > 0) {
      int z = x;
      x = parent[x];
      parent[z] = root;
    }

    return root;
  }

  /**
   */
  public int size() {
    return parent.length - 1;
  }

}
