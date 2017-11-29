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
public class MutableSparseLongSetFactory implements MutableLongSetFactory {

  /**
   * @param set
   * @throws IllegalArgumentException  if set is null
   */
  @Override
  public MutableLongSet make(long[] set) {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    if (set.length == 0) {
      return new MutableSparseLongSet();
    } else {
      // XXX not very efficient.
      TreeSet<Long> T = new TreeSet<>();
      for (long element : set) {
        T.add(Long.valueOf(element));
      }
      long[] copy = new long[T.size()];
      int i = 0;
      for (Long I : T) {
        copy[i++] = I.longValue();
      }
      MutableSparseLongSet result = new MutableSparseLongSet(copy);
      return result;
    }
  }

  @Override
  public MutableLongSet parse(String string) throws NumberFormatException {
    int[] backingStore = SparseIntSet.parseIntArray(string);
    long[] bs = new long[ backingStore.length ];
    for(int i = 0; i < bs.length; i++) bs[i] = backingStore[i];
    return new MutableSparseLongSet(bs);
  }

  /*
   * @see com.ibm.wala.util.intset.MutableLongSetFactory#make(com.ibm.wala.util.intset.LongSet)
   */
  @Override
  public MutableLongSet makeCopy(LongSet x) throws IllegalArgumentException {
    if (x == null) {
      throw new IllegalArgumentException("x == null");
    }
    return MutableSparseLongSet.make(x);
  }

  /*
   * @see com.ibm.wala.util.intset.MutableLongSetFactory#make()
   */
  @Override
  public MutableLongSet make() {
    return new MutableSparseLongSet();
  }

}
