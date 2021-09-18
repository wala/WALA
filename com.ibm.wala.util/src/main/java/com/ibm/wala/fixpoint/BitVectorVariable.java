/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.fixpoint;

import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSharedBitVectorIntSet;

/** A bit vector variable for dataflow analysis. */
public class BitVectorVariable extends AbstractVariable<BitVectorVariable> {

  private MutableSharedBitVectorIntSet V;

  public BitVectorVariable() {}

  /** @see com.ibm.wala.fixpoint.IVariable#copyState(com.ibm.wala.fixpoint.IVariable) */
  @Override
  public void copyState(BitVectorVariable other) {
    if (other == null) {
      throw new IllegalArgumentException("null other");
    }
    if (V == null) {
      if (other.V != null) {
        V = new MutableSharedBitVectorIntSet(other.V);
      }
      return;
    }
    if (other.V != null) {
      V.copySet(other.V);
    } else {
      V = null;
    }
  }

  /** Add all the bits in B to this bit vector */
  public void addAll(BitVector B) {
    if (B == null) {
      throw new IllegalArgumentException("null B");
    }
    if (V == null) {
      V = new MutableSharedBitVectorIntSet(new BitVectorIntSet(B));
      return;
    } else {
      V.addAll(new BitVectorIntSet(B));
    }
  }

  /** Add all the bits from other to this bit vector */
  public void addAll(BitVectorVariable other) {
    if (other == null) {
      throw new IllegalArgumentException("null other");
    }
    if (V == null) {
      copyState(other);
    } else {
      if (other.V != null) {
        V.addAll(other.V);
      }
    }
  }

  /** Does this variable have the same value as another? */
  public boolean sameValue(BitVectorVariable other) {
    if (other == null) {
      throw new IllegalArgumentException("null other");
    }
    if (V == null) {
      return (other.V == null);
    } else {
      if (other.V == null) {
        return false;
      } else {
        return V.sameValue(other.V);
      }
    }
  }

  @Override
  public String toString() {
    if (V == null) {
      return "[Empty]";
    }
    return V.toString();
  }

  /**
   * Set a particular bit
   *
   * @param b the bit to set
   */
  public void set(int b) {
    if (b < 0) {
      throw new IllegalArgumentException("illegal b: " + b);
    }
    if (V == null) {
      V = new MutableSharedBitVectorIntSet();
    }
    V.add(b);
  }

  /**
   * Is a particular bit set?
   *
   * @param b the bit to check
   */
  public boolean get(int b) {
    if (V == null) {
      return false;
    } else {
      return V.contains(b);
    }
  }

  /** @return the value of this variable as a bit vector ... null if the bit vector is empty. */
  public IntSet getValue() {
    return V;
  }

  public void clear(int i) {
    if (V != null) {
      V.remove(i);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  public int populationCount() {
    if (V == null) {
      return 0;
    } else {
      return V.size();
    }
  }
}
