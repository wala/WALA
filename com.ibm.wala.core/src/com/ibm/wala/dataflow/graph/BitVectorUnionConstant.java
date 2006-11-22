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
import com.ibm.wala.util.debug.Assertions;


/**
 * Operator OUT = IN U c
 */
public class BitVectorUnionConstant extends UnaryOperator {
  
  private final int c;
  public BitVectorUnionConstant(int c) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(c >= 0);
    }
    this.c = c;
  }
  

  /* (non-Javadoc)
   */
  public byte evaluate(IVariable lhs, IVariable rhs) {
    BitVectorVariable L = (BitVectorVariable) lhs;
    BitVectorVariable R = (BitVectorVariable) rhs;

    BitVectorVariable U = new BitVectorVariable();
    U.copyState(L);
    U.addAll(R);
    U.set(c);
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
  public String toString() {
    return "U " + c;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#hashCode()
   */
  public int hashCode() {
    return 9901 * c;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if (o instanceof BitVectorUnionConstant) {
      BitVectorUnionConstant other = (BitVectorUnionConstant)o;
      return c == other.c;
    } else {
      return false;
    }
  }
}