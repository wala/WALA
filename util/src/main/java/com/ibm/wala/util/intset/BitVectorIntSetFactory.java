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

import java.util.TreeSet;

/** */
public class BitVectorIntSetFactory implements MutableIntSetFactory<BitVectorIntSet> {

  /** @throws IllegalArgumentException if set is null */
  @Override
  public BitVectorIntSet make(int[] set) {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    if (set.length == 0) {
      return new BitVectorIntSet();
    } else {
      // XXX not very efficient.
      TreeSet<Integer> T = new TreeSet<>();
      for (int element : set) {
        T.add(element);
      }
      BitVectorIntSet result = new BitVectorIntSet();
      for (Integer I : T) {
        result.add(I);
      }
      return result;
    }
  }

  @Override
  public BitVectorIntSet parse(String string) throws NumberFormatException {
    int[] data = SparseIntSet.parseIntArray(string);
    BitVectorIntSet result = new BitVectorIntSet();
    for (int element : data) {
      result.add(element);
    }
    return result;
  }

  @Override
  public BitVectorIntSet makeCopy(IntSet x) throws IllegalArgumentException {
    if (x == null) {
      throw new IllegalArgumentException("x == null");
    }
    return new BitVectorIntSet(x);
  }

  @Override
  public BitVectorIntSet make() {
    return new BitVectorIntSet();
  }
}
