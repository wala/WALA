/*
 * Copyright (c) 2008 IBM Corporation.
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

/** Just kills everything */
public class BitVectorKillAll extends UnaryOperator<BitVectorVariable> {

  private static final BitVectorKillAll SINGLETON = new BitVectorKillAll();

  public static BitVectorKillAll instance() {
    return SINGLETON;
  }

  private BitVectorKillAll() {}

  /* (non-Javadoc)
   * @see com.ibm.wala.fixedpoint.impl.UnaryOperator#evaluate(com.ibm.wala.fixpoint.IVariable, com.ibm.wala.fixpoint.IVariable)
   */
  @Override
  public byte evaluate(BitVectorVariable lhs, BitVectorVariable rhs) {
    BitVectorVariable empty = new BitVectorVariable();
    if (!lhs.sameValue(empty)) {
      lhs.copyState(empty);
      return CHANGED;
    } else {
      return NOT_CHANGED;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#hashCode()
   */
  @Override
  public int hashCode() {
    return 12423958;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.fixedpoint.impl.AbstractOperator#toString()
   */
  @Override
  public String toString() {
    return "KillAll";
  }
}
