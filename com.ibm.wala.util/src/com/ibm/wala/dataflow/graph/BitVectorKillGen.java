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
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;

/**
 * Operator OUT = (IN - kill) U gen
 */
public class BitVectorKillGen extends UnaryOperator<BitVectorVariable> {

  private final BitVectorIntSet kill;

  private final BitVectorIntSet gen;

  public BitVectorKillGen(BitVector kill, BitVector gen) {
    if (kill == null) {
      throw new IllegalArgumentException("null kill");
    }
    if (gen == null) {
      throw new IllegalArgumentException("null gen");
    }
    this.kill = new BitVectorIntSet(kill);
    this.gen = new BitVectorIntSet(gen);
  }

  @Override
  public byte evaluate(BitVectorVariable lhs, BitVectorVariable rhs) throws IllegalArgumentException {
    if (rhs == null) {
      throw new IllegalArgumentException("rhs == null");
    }
    if (lhs == null) {
      throw new IllegalArgumentException("lhs == null");
    }
    BitVectorVariable U = new BitVectorVariable();
    BitVectorIntSet bv = new BitVectorIntSet();
    if (rhs.getValue() != null) {
      bv.addAll(rhs.getValue());
    }
    bv.removeAll(kill);
    bv.addAll(gen);
    U.addAll(bv.getBitVector());
    if (!lhs.sameValue(U)) {
      lhs.copyState(U);
      return CHANGED;
    } else {
      return NOT_CHANGED;
    }
  }

  @Override
  public String toString() {
    return "GenKill";
  }

  @Override
  public int hashCode() {
    return 9901 * kill.hashCode() + 1213 * gen.hashCode();
  }

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
