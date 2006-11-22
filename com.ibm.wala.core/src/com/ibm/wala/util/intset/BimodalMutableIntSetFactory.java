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
 * 
 * An object that creates some bimodal mutable int sets.
 * 
 * @author sfink
 */
public class BimodalMutableIntSetFactory implements MutableIntSetFactory {

  private final MutableSparseIntSetFactory factory = new MutableSparseIntSetFactory();

  /**
   * @param set
   */
  public MutableIntSet make(int[] set) {
    BimodalMutableIntSet result = new BimodalMutableIntSet();
    result.impl = factory.make(set);
    return result;
  }

  /**
   * @param string
   */
  public MutableIntSet parse(String string) {
    BimodalMutableIntSet result = new BimodalMutableIntSet();
    result.impl = factory.parse(string);
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make(com.ibm.wala.util.intset.IntSet)
   */
  public MutableIntSet makeCopy(IntSet x) {
    return BimodalMutableIntSet.makeCopy(x);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make()
   */
  public MutableIntSet make() {
    return new BimodalMutableIntSet();
  }
}