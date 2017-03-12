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

import com.ibm.wala.util.debug.UnimplementedError;

/**
 * An object that creates some bimodal mutable int sets.
 */
public class BimodalMutableIntSetFactory implements MutableIntSetFactory<BimodalMutableIntSet> {

  private final MutableSparseIntSetFactory factory = new MutableSparseIntSetFactory();

  /**
   * @param set
   */
  @Override
  public BimodalMutableIntSet make(int[] set) {
    BimodalMutableIntSet result = new BimodalMutableIntSet();
    result.impl = factory.make(set);
    return result;
  }

  /**
   * @param string
   */
  @Override
  public BimodalMutableIntSet parse(String string) throws NumberFormatException {
    BimodalMutableIntSet result = new BimodalMutableIntSet();
    result.impl = factory.parse(string);
    return result;
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public BimodalMutableIntSet makeCopy(IntSet x) throws UnimplementedError, IllegalArgumentException {
    if (x == null) {
      throw new IllegalArgumentException("x == null");
    }
    return BimodalMutableIntSet.makeCopy(x);
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make()
   */
  @Override
  public BimodalMutableIntSet make() {
    return new BimodalMutableIntSet();
  }
}
