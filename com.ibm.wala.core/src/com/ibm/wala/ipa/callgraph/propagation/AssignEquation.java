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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.fixedpoint.impl.AbstractOperator;
import com.ibm.wala.fixedpoint.impl.UnaryStatement;
import com.ibm.wala.fixpoint.IVariable;

/**
 * A specialized equation class introduced for efficiency
 * 
 * @author sfink
 */
public final class AssignEquation extends UnaryStatement {

  AssignEquation(IVariable lhs, IVariable rhs) {
    super(lhs, rhs);
  }

  @Override
  public AbstractOperator getOperator() {
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
