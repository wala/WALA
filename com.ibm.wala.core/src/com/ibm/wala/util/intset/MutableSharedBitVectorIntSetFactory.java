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
 * A factory for mutable shared bit vector int sets
 * 
 * @author sfink
 */
public class MutableSharedBitVectorIntSetFactory implements MutableIntSetFactory {

  private final MutableSparseIntSetFactory sparseFactory = new MutableSparseIntSetFactory();

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make(int[])
   */
  public MutableIntSet make(int[] set) {
    SparseIntSet s = (SparseIntSet) sparseFactory.make(set);
    return new MutableSharedBitVectorIntSet(s);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#parse(java.lang.String)
   */
  public MutableIntSet parse(String string) throws NumberFormatException {
    SparseIntSet s = (SparseIntSet) sparseFactory.parse(string);
    return new MutableSharedBitVectorIntSet(s);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#makeCopy(com.ibm.wala.util.intset.IntSet)
   */
  public MutableIntSet makeCopy(IntSet x) {
    if (x instanceof MutableSharedBitVectorIntSet) {
      return new MutableSharedBitVectorIntSet((MutableSharedBitVectorIntSet) x);
    } else if (x instanceof SparseIntSet) {
      return new MutableSharedBitVectorIntSet((SparseIntSet) x);
    } else if (x instanceof BitVectorIntSet) {
      return new MutableSharedBitVectorIntSet((BitVectorIntSet) x);
    } else if (x instanceof DebuggingMutableIntSet) {
      return new MutableSharedBitVectorIntSet(new SparseIntSet(x));
    } else {
      Assertions.UNREACHABLE("unexpected type " + x.getClass());
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make()
   */
  public MutableIntSet make() {
    return new MutableSharedBitVectorIntSet();
  }

}