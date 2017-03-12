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


/**
 * Operator U(n) = U(n) | U(j)
 */
public final class UnaryOr extends UnaryOperator<BooleanVariable> {

  private static final UnaryOr SINGLETON = new UnaryOr();

  public static UnaryOr instance() {
    return UnaryOr.SINGLETON;
  }

  private UnaryOr() {
  }

  @Override
  public byte evaluate(BooleanVariable lhs, BooleanVariable rhs) throws IllegalArgumentException {
    if (lhs == null) {
      throw new IllegalArgumentException("lhs == null");
    }

    boolean old = lhs.getValue();
    lhs.or(rhs);
    return (lhs.getValue() != old) ? FixedPointConstants.CHANGED : FixedPointConstants.NOT_CHANGED;
  }

  @Override
  public String toString() {
    return "OR";
  }

  @Override
  public int hashCode() {
    return 9887;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof UnaryOr);
  }
}
