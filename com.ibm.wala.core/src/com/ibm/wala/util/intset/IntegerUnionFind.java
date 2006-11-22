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

import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * An implementation of Tarjan's union-find, using path compression and
 * balancing, for non-negative integers
 * 
 * @author sfink
 */
public class IntegerUnionFind {

  final private static int DEFAULT_SIZE = 100;

  /**
   * 
   * parent[i+1] =
   * <ul>
   * <li> j > 0 if i is in the same equiv class as j
   * <li> j <= 0 if i is the representative of a class of size -(j)+1
   * </ul>
   * 
   * we initialize parent[j] = 0, so each element is a class of size 1
   */
  int[] parent;

  public IntegerUnionFind() {
    this(DEFAULT_SIZE);
  }

  /**
   * @param size
   */
  public IntegerUnionFind(int size) {
    parent = new int[size + 1];
  }

  /**
   * union the equiv classes of x and y
   * 
   * @param x
   * @param y
   */
  public void union(int x, int y) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(x >= 0);
      Assertions._assert(y >= 0);
    }
    if (x >= size() || y >= size()) {
      grow(2*Math.max(x, y));
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
    parent = new int[size+1];
    System.arraycopy(old,0,parent,0,old.length);
  }

  /**
   * @param x
   * @return representative of x's equivalence class
   */
  public int find(int x) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(x >= 0);
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