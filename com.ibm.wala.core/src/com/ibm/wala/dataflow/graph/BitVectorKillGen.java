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
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;

/**
 * Operator OUT = (IN - kill) U gen
 */
public class BitVectorKillGen extends UnaryOperator {

  private final BitVectorIntSet kill;

  private final BitVectorIntSet gen;

  public BitVectorKillGen(BitVector kill, BitVector gen) {
    this.kill = new BitVectorIntSet(kill);
    this.gen = new BitVectorIntSet(gen);
  }

  /*
   * (non-Javadoc)
   */
  @Override
  public byte evaluate(IVariable lhs, IVariable rhs) {
    BitVectorVariable L = (BitVectorVariable) lhs;
    BitVectorVariable R = (BitVectorVariable) rhs;

    BitVectorVariable U = new BitVectorVariable();
    BitVectorIntSet bv = new BitVectorIntSet();
    if (R.getValue() != null) {
      bv.addAll(R.getValue());
    }
    bv.removeAll(kill);
    bv.addAll(gen);
    U.addAll(bv.getBitVector());
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
  @Override
  public String toString() {
    return "GenKill";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.Operator#hashCode()
   */
  @Override
  public int hashCode() {
    return 9901 * kill.hashCode() + 1213 * gen.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof BitVectorKillGen) {
      BitVectorKillGen other = (BitVectorKillGen) o;
      return kill.sameValue(other.kill) && gen.sameValue(other.gen);
    } else {
      return false;
    }
  }
}