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
import com.ibm.wala.util.intset.IntSet;

/**
 * Operator OUT = IN - filterSet
 */
public class BitVectorFilter extends UnaryOperator {

  private final BitVectorIntSet mask;

  public BitVectorFilter(BitVector mask) {
    this.mask = new BitVectorIntSet(mask);
  }

  /*
   * (non-Javadoc)
   */
  @Override
  public byte evaluate(IVariable lhs, IVariable rhs) {
    BitVectorVariable L = (BitVectorVariable) lhs;
    BitVectorVariable R = (BitVectorVariable) rhs;

    BitVectorVariable U = new BitVectorVariable();
    U.copyState(L);

    IntSet r = R.getValue();
    if (r == null)
      return NOT_CHANGED;

    BitVectorIntSet rr = new BitVectorIntSet();
    rr.addAll(r);
    rr.removeAll(mask);

    System.err.println("adding " + rr + " to " + L);

    U.addAll(rr.getBitVector());

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
    return "U - " + mask;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.Operator#hashCode()
   */
  @Override
  public int hashCode() {
    return 29 * mask.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof BitVectorFilter) {
      BitVectorFilter other = (BitVectorFilter) o;
      return mask.equals(other.mask);
    } else {
      return false;
    }
  }
}
