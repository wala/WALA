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

import java.util.Iterator;
import java.util.TreeSet;

/**
 * 
 * An object that creates mutable sparse int sets.
 * 
 * @author sfink
 */
public class MutableSparseIntSetFactory implements MutableIntSetFactory {

  /**
   * @param set
   * @throws IllegalArgumentException  if set is null
   */
  public MutableIntSet make(int[] set) {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    if (set.length == 0) {
      return new MutableSparseIntSet();
    } else {
      // XXX not very efficient.
      TreeSet<Integer> T = new TreeSet<Integer>();
      for (int i = 0; i < set.length; i++) {
        T.add(new Integer(set[i]));
      }
      int[] copy = new int[T.size()];
      int i = 0;
      for (Iterator<Integer> it = T.iterator(); it.hasNext();) {
        Integer I = it.next();
        copy[i++] = I.intValue();
      }
      MutableSparseIntSet result = new MutableSparseIntSet(copy);
      return result;
    }
  }

  /**
   * @param string
   */
  public MutableIntSet parse(String string) throws NumberFormatException {
    int[] backingStore = SparseIntSet.parseIntArray(string);
    return new MutableSparseIntSet(backingStore);
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make(com.ibm.wala.util.intset.IntSet)
   */
  public MutableIntSet makeCopy(IntSet x) throws IllegalArgumentException {
    if (x == null) {
      throw new IllegalArgumentException("x == null");
    }
    return MutableSparseIntSet.make(x);
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make()
   */
  public MutableIntSet make() {
    return new MutableSparseIntSet();
  }

}
