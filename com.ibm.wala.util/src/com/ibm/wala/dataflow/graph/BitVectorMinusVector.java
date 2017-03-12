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
package com.ibm.wala.dataflow.graph;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;


/**
 * Operator OUT = IN / v
 */
public class BitVectorMinusVector extends UnaryOperator<BitVectorVariable> {
  
  private final BitVectorIntSet v;
  public BitVectorMinusVector(BitVector v) {
    this.v = new BitVectorIntSet(v);
  }
  

  @Override
  public byte evaluate(BitVectorVariable lhs, BitVectorVariable rhs) throws IllegalArgumentException, IllegalArgumentException {

    if (lhs == null) {
      throw new IllegalArgumentException("lhs == null");
    }
    if (rhs == null) {
      throw new IllegalArgumentException("rhs == null");
    }
    BitVectorVariable U = new BitVectorVariable();
    BitVectorIntSet bv = new BitVectorIntSet();
    bv.addAll(rhs.getValue());
    bv.removeAll(v);
    U.addAll(bv.getBitVector());
    if (!lhs.sameValue(U)) {
      lhs.copyState(U);
      return CHANGED;
    } else {
      return NOT_CHANGED;
    }
  }

  @Override
  public String toString() {
    return "U " + v;
  }

  @Override
  public int hashCode() {
    return 9901 * v.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BitVectorMinusVector) {
      BitVectorMinusVector other = (BitVectorMinusVector)o;
      return v.sameValue(other.v);
    } else {
      return false;
    }
  }
}
