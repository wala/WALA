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


/**
 * Operator lhs = lhs U rhs U v
 */
public class BitVectorUnionVector extends UnaryOperator {
  
  private final BitVector v;
  public BitVectorUnionVector(BitVector v) {
    this.v = v;
  }
  

  /* (non-Javadoc)
   */
  @Override
  public byte evaluate(IVariable lhs, IVariable rhs) {
    BitVectorVariable L = (BitVectorVariable) lhs;
    BitVectorVariable R = (BitVectorVariable) rhs;

    BitVectorVariable U = new BitVectorVariable();
    U.copyState(L);
    U.addAll(R);
    U.addAll(v);
    if (!L.sameValue(U)) {
      L.copyState(U);
      return CHANGED;
    } else {
      return NOT_CHANGED;
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "U " + v;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#hashCode()
   */
  @Override
  public int hashCode() {
    return 9901 * v.hashCode();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof BitVectorUnionVector) {
      BitVectorUnionVector other = (BitVectorUnionVector)o;
      return v.sameBits(other.v);
    } else {
      return false;
    }
  }
}