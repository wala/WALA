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


/**
 * Operator OUT = IN U v
 */
public class BitVectorOr extends UnaryOperator<BitVectorVariable> {
  
  private final BitVector v;
  public BitVectorOr(BitVector v) {
    if (v == null) {
      throw new IllegalArgumentException("null v");
    }
    this.v = v;
  }
  

  @Override
  public byte evaluate(BitVectorVariable lhs, BitVectorVariable rhs) throws IllegalArgumentException {
    if (lhs == null) {
      throw new IllegalArgumentException("lhs == null");
    }
    if (rhs == null) {
      throw new IllegalArgumentException("rhs == null");
    }
    BitVectorVariable U = new BitVectorVariable();
    U.copyState(lhs);
    U.addAll(rhs);
    U.addAll(v);
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
    if (o instanceof BitVectorOr) {
      BitVectorOr other = (BitVectorOr)o;
      return v.equals(other.v);
    } else {
      return false;
    }
  }
}
