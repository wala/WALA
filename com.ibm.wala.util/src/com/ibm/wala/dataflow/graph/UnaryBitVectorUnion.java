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
 * Operator U(n) = U(n) U U(j)
 */
public class UnaryBitVectorUnion extends UnaryOperator<BitVectorVariable> {
  
  private final static UnaryBitVectorUnion SINGLETON = new UnaryBitVectorUnion();
  
  public static UnaryBitVectorUnion instance() {
    return SINGLETON;
  }
  
  private UnaryBitVectorUnion() {
  }
  

  @Override
  public byte evaluate(BitVectorVariable lhs, BitVectorVariable rhs) throws IllegalArgumentException {
    if (lhs == null) {
      throw new IllegalArgumentException("lhs == null");
    }

    BitVectorVariable U = new BitVectorVariable();
    U.copyState(lhs);
    U.addAll(rhs);
    if (lhs.sameValue(U)) {
      return NOT_CHANGED;
    } else {
      lhs.copyState(U);
      return CHANGED;
    }
  }

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
    return (o instanceof UnaryBitVectorUnion);
  }
}
