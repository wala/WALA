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

import com.ibm.wala.fixedpoint.impl.AbstractVariable;
import com.ibm.wala.util.DeterministicHashCode;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSharedBitVectorIntSet;

/**
 *
 * A BitVector variable for dataflow analysis.
 * 
 * @author sfink
 */
public class BitVectorVariable extends AbstractVariable {

  private MutableSharedBitVectorIntSet V;
  private final int hash;

  public BitVectorVariable() {
    this.hash = DeterministicHashCode.get();
  }

  public void copyState(BitVectorVariable other) {
    if (V == null) {
      if (other.V == null) {
        return;
      } else {
        V = new MutableSharedBitVectorIntSet(other.V);
        return;
      }
    }
    if (other.V != null) {
      V.copySet(other.V);
    }
  }

  public void addAll(BitVector B) {
    if (V == null) {
      V = new MutableSharedBitVectorIntSet(new BitVectorIntSet(B));
      return;
    } else {
      V.addAll(new BitVectorIntSet(B));
    }
  }

  public void addAll(BitVectorVariable other) {
    if (V == null) {
      copyState(other);
    } else {
      if (other.V != null) {
        V.addAll(other.V);
      }
    }
  }

  public boolean sameValue(BitVectorVariable other) {
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

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (V == null) {
      return "[Empty]";
    }
    return V.toString();
  }

  /**
   * Set a particular bit
   * @param b the bit to set
   */
  public void set(int b) {
    if (V == null) {
      V = new MutableSharedBitVectorIntSet();
    }
    V.add(b);
  }
  /**
   * Is a particular bit set?
   * @param b the bit to check
   */
  public boolean get(int b) {
    if (V == null) {
      return false;
    } else {
      return V.contains(b);
    }
  }

  /**
   * @return the value of this variable as a bit vector ... null if the 
   * bit vector is empty.
   */
  public IntSet getValue() {
    return V;
  }

  public void clear(int i) {
    if (V != null) {
      V.remove(i);
    }
  }

  @Override
  public int hashCode() {
    return hash;
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
