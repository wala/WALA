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

import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;


/**
 * Operator OUT = IN / v
 */
public class BitVectorMinusVector extends UnaryOperator {
  
  private final BitVectorIntSet v;
  public BitVectorMinusVector(BitVector v) {
    this.v = new BitVectorIntSet(v);
  }
  

  @Override
  public byte evaluate(IVariable lhs, IVariable rhs) {
    BitVectorVariable L = (BitVectorVariable) lhs;
    BitVectorVariable R = (BitVectorVariable) rhs;

    BitVectorVariable U = new BitVectorVariable();
    BitVectorIntSet bv = new BitVectorIntSet();
    bv.addAll(R.getValue());
    bv.removeAll(v);
    U.addAll(bv.getBitVector());
    if (!L.sameValue(U)) {
      L.copyState(U);
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