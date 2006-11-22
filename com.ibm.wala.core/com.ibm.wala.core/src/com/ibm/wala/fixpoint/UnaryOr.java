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
package com.ibm.wala.fixpoint;

import com.ibm.wala.fixedpoint.impl.UnaryOperator;



/**
 * Operator U(n) = U(n) | U(j)
 */
public final class UnaryOr extends UnaryOperator {

  private static final UnaryOr SINGLETON = new UnaryOr();
  
  public static UnaryOr instance() {
    return UnaryOr.SINGLETON;
  }

  private UnaryOr() {
  }


  /* (non-Javadoc)
   */
  public byte evaluate(IVariable lhs, IVariable rhs) {
    BooleanVariable L = (BooleanVariable) lhs;
    BooleanVariable R = (BooleanVariable) rhs;

    boolean old = L.getValue();
    L.or(R);
    return (L.getValue() != old) ? FixedPointConstants.CHANGED : FixedPointConstants.NOT_CHANGED;
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "OR";
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#hashCode()
   */
  public int hashCode() {
    return 9887;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    return (o instanceof UnaryOr);
  }
}