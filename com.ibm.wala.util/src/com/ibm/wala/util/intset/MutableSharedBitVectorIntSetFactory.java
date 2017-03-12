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
 * A factory for mutable shared bit vector int sets
 */
public class MutableSharedBitVectorIntSetFactory implements MutableIntSetFactory<MutableSharedBitVectorIntSet> {

  private final MutableSparseIntSetFactory sparseFactory = new MutableSparseIntSetFactory();

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make(int[])
   */
  @Override
  public MutableSharedBitVectorIntSet make(int[] set) {
    SparseIntSet s = sparseFactory.make(set);
    return new MutableSharedBitVectorIntSet(s);
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#parse(java.lang.String)
   */
  @Override
  public MutableSharedBitVectorIntSet parse(String string) throws NumberFormatException {
    SparseIntSet s = sparseFactory.parse(string);
    return new MutableSharedBitVectorIntSet(s);
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#makeCopy(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public MutableSharedBitVectorIntSet makeCopy(IntSet x) throws IllegalArgumentException {
    if (x == null) {
      throw new IllegalArgumentException("x == null");
    }
    if (x instanceof MutableSharedBitVectorIntSet) {
      return new MutableSharedBitVectorIntSet((MutableSharedBitVectorIntSet) x);
    } else if (x instanceof SparseIntSet) {
      return new MutableSharedBitVectorIntSet((SparseIntSet) x);
    } else if (x instanceof BitVectorIntSet) {
      return new MutableSharedBitVectorIntSet((BitVectorIntSet) x);
    } else if (x instanceof DebuggingMutableIntSet) {
      return new MutableSharedBitVectorIntSet(new SparseIntSet(x));
    } else {
      // really slow.  optimize as needed.
      MutableSharedBitVectorIntSet result = new MutableSharedBitVectorIntSet();
      for (IntIterator it = x.intIterator(); it.hasNext(); ) {
        result.add(it.next());
      }
      return result;
    }
  }

  /*
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make()
   */
  @Override
  public MutableSharedBitVectorIntSet make() {
    return new MutableSharedBitVectorIntSet();
  }

}
