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

import java.util.TreeSet;

/**
 * An object that creates mutable sparse int sets.
 */
public class MutableSparseIntSetFactory implements MutableIntSetFactory<MutableSparseIntSet> {

  /**
   * @throws IllegalArgumentException  if set is null
   */
  @Override
  public MutableSparseIntSet make(int[] set) {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    if (set.length == 0) {
      return MutableSparseIntSet.makeEmpty();
    } else {
      // XXX not very efficient.
      TreeSet<Integer> T = new TreeSet<>();
      for (int element : set) {
        T.add(element);
      }
      int[] copy = new int[T.size()];
      int i = 0;
      for (Integer I : T) {
        copy[i++] = I.intValue();
      }
      MutableSparseIntSet result = new MutableSparseIntSet(copy);
      return result;
    }
  }

  /**
   * @param string
   */
  @Override
  public MutableSparseIntSet parse(String string) throws NumberFormatException {
    int[] backingStore = SparseIntSet.parseIntArray(string);
    return new MutableSparseIntSet(backingStore);
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public MutableSparseIntSet makeCopy(IntSet x) throws IllegalArgumentException {
    if (x == null) {
      throw new IllegalArgumentException("x == null");
    }
    return MutableSparseIntSet.make(x);
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make()
   */
  @Override
  public MutableSparseIntSet make() {
    return MutableSparseIntSet.makeEmpty();
  }

}
