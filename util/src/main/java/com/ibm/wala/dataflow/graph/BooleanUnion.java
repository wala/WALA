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
package com.ibm.wala.dataflow.graph;

import static com.google.common.base.Preconditions.checkArgument;

import com.ibm.wala.fixpoint.BooleanVariable;
import org.jspecify.annotations.Nullable;

/** Operator U(n) = U(n) U U(j) */
public class BooleanUnion extends AbstractMeetOperator<BooleanVariable> {

  private static final BooleanUnion SINGLETON = new BooleanUnion();

  public static BooleanUnion instance() {
    return SINGLETON;
  }

  private BooleanUnion() {}

  @Override
  public String toString() {
    return "UNION";
  }

  @Override
  public int hashCode() {
    return 9901;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return (o instanceof BooleanUnion);
  }

  @Override
  public byte evaluate(BooleanVariable lhs, BooleanVariable[] rhs) throws NullPointerException {
    checkArgument(rhs != null, "null rhs");
    BooleanVariable U = new BooleanVariable();
    U.copyState(lhs);
    for (BooleanVariable R : rhs) {
      if (R != null) {
        U.or(R);
      }
    }
    if (!lhs.sameValue(U)) {
      lhs.copyState(U);
      return CHANGED;
    } else {
      return NOT_CHANGED;
    }
  }
}
