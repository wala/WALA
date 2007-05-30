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

import com.ibm.wala.fixpoint.BooleanVariable;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.IVariable;

/**
 * Operator U(n) = U(n) U U(j)
 */
public class BooleanUnion extends AbstractMeetOperator implements FixedPointConstants {

  private final static BooleanUnion SINGLETON = new BooleanUnion();

  public static BooleanUnion instance() {
    return SINGLETON;
  }

  private BooleanUnion() {
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "UNION";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.Operator#hashCode()
   */
  @Override
  public int hashCode() {
    return 9901;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    return (o instanceof BooleanUnion);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.Operator#evaluate(com.ibm.wala.dataflow.fixpoint.IVariable[])
   */
  @Override
  public byte evaluate(IVariable lhs, IVariable[] rhs) throws NullPointerException {
    BooleanVariable L = (BooleanVariable) lhs;
    BooleanVariable U = new BooleanVariable(L.hashCode());
    U.copyState(L);
    for (int i = 0; i < rhs.length; i++) {
      BooleanVariable R = (BooleanVariable) rhs[i];
      if (R != null) {
        U.or(R);
      }
    }
    if (!L.sameValue(U)) {
      L.copyState(U);
      return CHANGED;
    } else {
      return NOT_CHANGED;
    }
  }
}