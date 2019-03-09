/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.dataflow.graph;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;

/** Operator OUT = IN - filterSet */
public class BitVectorFilter extends UnaryOperator<BitVectorVariable> {

  private final BitVectorIntSet mask;

  public BitVectorFilter(BitVector mask) {
    if (mask == null) {
      throw new IllegalArgumentException("null mask");
    }
    this.mask = new BitVectorIntSet(mask);
  }

  @Override
  public byte evaluate(BitVectorVariable lhs, BitVectorVariable rhs)
      throws IllegalArgumentException {
    if (rhs == null) {
      throw new IllegalArgumentException("rhs == null");
    }
    BitVectorVariable U = new BitVectorVariable();
    U.copyState(lhs);

    IntSet r = rhs.getValue();
    if (r == null) {
      return NOT_CHANGED;
    }

    BitVectorIntSet rr = new BitVectorIntSet();
    rr.addAll(r);
    rr.removeAll(mask);

    System.err.println("adding " + rr + " to " + lhs);

    U.addAll(rr.getBitVector());

    if (!lhs.sameValue(U)) {
      lhs.copyState(U);
      return CHANGED;
    } else {
      return NOT_CHANGED;
    }
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    return "U - " + mask;
  }

  @Override
  public int hashCode() {
    return 29 * mask.hashCode();
  }

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
