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


/**
 *
 * A boolean variable for dataflow analysis.
 * 
 * @author sfink
 */
public class BooleanVariable extends AbstractVariable {

  private boolean B;
  private final int hash;

  /**
   * Default constructor
   */
  public BooleanVariable(int hash) {
    this.hash = hash;
  }

  /**
   * @param b initial value for this variable
   */
  public BooleanVariable(boolean b, int hash) {
    this.B = b;
    this.hash = hash;
  }

  /* (non-Javadoc)
   */
  public void copyState(IVariable v) throws NullPointerException {
    BooleanVariable other = (BooleanVariable) v;
    B = other.B;
  }


  public boolean sameValue(BooleanVariable other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    return B == other.B;
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return hash + (B ? "[TRUE]" : "[FALSE]");
  }
  /**
   * @return the value of this variable 
   */
  public boolean getValue() {
    return B;
  }

  /**
   * @param other
   * @throws IllegalArgumentException  if other is null
   */
  public void or(BooleanVariable other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    B = B | other.B;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.AbstractVariable#hashCode()
   */
  public int hashCode() {
    return hash;
  }

  /**
   * @param b
   */
  public void set(boolean b) {
    B = b;
  }

  /* (non-Javadoc)
   */
  public boolean equals(Object obj) {
    return this == obj;
  }

}
