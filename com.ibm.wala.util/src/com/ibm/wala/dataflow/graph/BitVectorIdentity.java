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
 * Operator OUT = IN
 */
public class BitVectorIdentity extends UnaryOperator<BitVectorVariable> {

  private final static BitVectorIdentity SINGLETON = new BitVectorIdentity();

  public static BitVectorIdentity instance() {
    return SINGLETON;
  }

  private BitVectorIdentity() {
  }

  @Override
  public byte evaluate(BitVectorVariable lhs, BitVectorVariable rhs) throws IllegalArgumentException  {
    if (lhs == null) {
      throw new IllegalArgumentException("lhs cannot be null");
    }
    
    if (lhs.sameValue(rhs)) {
      return NOT_CHANGED;
    } else {
      lhs.copyState(rhs);
      return CHANGED;
    }
  }

  @Override
  public String toString() {
    return "Id ";
  }

  @Override
  public int hashCode() {
    return 9902;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof BitVectorIdentity);
  }

  /* 
   * @see com.ibm.wala.fixpoint.UnaryOperator#isIdentity()
   */
  @Override
  public boolean isIdentity() {
    return true;
  }
}
