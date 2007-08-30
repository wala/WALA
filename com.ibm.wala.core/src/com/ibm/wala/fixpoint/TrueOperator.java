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
 * Operator U(n) = true
 */
public final class TrueOperator extends UnaryOperator {

  private static final TrueOperator SINGLETON = new TrueOperator();

  public static TrueOperator instance() {
    return TrueOperator.SINGLETON;
  }

  private TrueOperator() {
  }

  @Override
  public byte evaluate(IVariable lhs, IVariable rhs) throws IllegalArgumentException {
    if (lhs == null) {
      throw new IllegalArgumentException("lhs == null");
    }
    BooleanVariable L = (BooleanVariable) lhs;
    if (L.getValue()) {
      return NOT_CHANGED;
    } else {
      L.set(true);
      return CHANGED;
    }
  }

  @Override
  public String toString() {
    return "TRUE";
  }

  @Override
  public int hashCode() {
    return 9889;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof TrueOperator);
  }
}