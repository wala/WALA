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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.fixpoint.UnaryStatement;

/** A specialized equation class introduced for efficiency. */
public final class AssignEquation extends UnaryStatement<PointsToSetVariable> {

  AssignEquation(PointsToSetVariable lhs, PointsToSetVariable rhs) {
    super(lhs, rhs);
  }

  @Override
  public UnaryOperator<PointsToSetVariable> getOperator() {
    return PropagationCallGraphBuilder.assignOperator;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof AssignEquation) {
      AssignEquation other = (AssignEquation) o;
      return getLHS().equals(other.getLHS()) && getRightHandSide().equals(other.getRightHandSide());
    } else {
      return false;
    }
  }
}
