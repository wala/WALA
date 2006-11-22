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


/**
 * Operator U(n) = U(n) U U(j)
 */
public class UnaryBitVectorUnion extends UnaryOperator {
  
  private final static UnaryBitVectorUnion SINGLETON = new UnaryBitVectorUnion();
  
  public static UnaryBitVectorUnion instance() {
    return SINGLETON;
  }
  
  private UnaryBitVectorUnion() {
  }
  

  /* (non-Javadoc)
   */
  public byte evaluate(IVariable lhs, IVariable rhs) {
    BitVectorVariable L = (BitVectorVariable) lhs;
    BitVectorVariable R = (BitVectorVariable) rhs;

    BitVectorVariable U = new BitVectorVariable();
    U.copyState(L);
    U.addAll(R);
    if (L.sameValue(U)) {
      return NOT_CHANGED;
    } else {
      L.copyState(U);
      return CHANGED;
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "UNION";
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#hashCode()
   */
  public int hashCode() {
    return 9901;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    return (o instanceof UnaryBitVectorUnion);
  }
}