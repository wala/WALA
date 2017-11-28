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

/**
 * Operator U(n) = U(n) U U(j)
 */
public class BitVectorUnion extends AbstractMeetOperator<BitVectorVariable> {

  private final static BitVectorUnion SINGLETON = new BitVectorUnion();

  public static BitVectorUnion instance() {
    return SINGLETON;
  }

  private BitVectorUnion() {
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "UNION";
  }

  @Override
  public int hashCode() {
    return 9901;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof BitVectorUnion);
  }

  /*
   * @see com.ibm.wala.dataflow.fixpoint.Operator#evaluate(com.ibm.wala.dataflow.fixpoint.IVariable[])
   */
  @Override
  public byte evaluate(BitVectorVariable lhs, BitVectorVariable[] rhs) throws IllegalArgumentException {
    if (lhs == null) {
      throw new IllegalArgumentException("null lhs");
    }
    if (rhs == null) {
      throw new IllegalArgumentException("rhs == null");
    }
    BitVectorVariable U = new BitVectorVariable();
    U.copyState(lhs);
    for (BitVectorVariable R : rhs) {
      U.addAll(R);
    }
    if (!lhs.sameValue(U)) {
      lhs.copyState(U);
      return CHANGED;
    } else {
      return NOT_CHANGED;
    }
  }
}
