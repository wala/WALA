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


/**
 * Operator OUT = IN U c
 */
public class BitVectorUnionConstant extends UnaryOperator<BitVectorVariable> {
  
  private final int c;
  public BitVectorUnionConstant(int c) {
    if (c < 0) {
      throw new IllegalArgumentException("Invalid c: " + c);
    }
    this.c = c;
  }
  

  @Override
  public byte evaluate(BitVectorVariable lhs, BitVectorVariable rhs) throws IllegalArgumentException {

    if (lhs == null) {
      throw new IllegalArgumentException("lhs == null");
    }
    BitVectorVariable U = new BitVectorVariable();
    U.copyState(lhs);
    U.addAll(rhs);
    U.set(c);
    if (!lhs.sameValue(U)) {
      lhs.copyState(U);
      return CHANGED;
    } else {
      return NOT_CHANGED;
    }
  }

  @Override
  public String toString() {
    return "U " + c;
  }

  @Override
  public int hashCode() {
    return 9901 * c;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BitVectorUnionConstant) {
      BitVectorUnionConstant other = (BitVectorUnionConstant)o;
      return c == other.c;
    } else {
      return false;
    }
  }
}
