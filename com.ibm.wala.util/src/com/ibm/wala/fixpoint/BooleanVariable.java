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

/** A boolean variable for dataflow analysis. */
public class BooleanVariable extends AbstractVariable<BooleanVariable> {

  private boolean B;

  public BooleanVariable() {}

  /** @param b initial value for this variable */
  public BooleanVariable(boolean b) {
    this.B = b;
  }

  @Override
  public void copyState(BooleanVariable other) {
    if (other == null) {
      throw new IllegalArgumentException("other null");
    }
    B = other.B;
  }

  public boolean sameValue(BooleanVariable other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    return B == other.B;
  }

  @Override
  public String toString() {
    return (B ? "[TRUE]" : "[FALSE]");
  }

  /** @return the value of this variable */
  public boolean getValue() {
    return B;
  }

  /** @throws IllegalArgumentException if other is null */
  public void or(BooleanVariable other) {
    if (other == null) {
      throw new IllegalArgumentException("other is null");
    }
    B = B || other.B;
  }

  public void set(boolean b) {
    B = b;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }
}
