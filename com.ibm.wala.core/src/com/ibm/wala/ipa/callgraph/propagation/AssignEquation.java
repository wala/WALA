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
import com.ibm.wala.fixpoint.IntSetVariable;
import com.ibm.wala.util.debug.VerboseAction;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableSharedBitVectorIntSet;

/**
 * 
 * A apecialized equation class introduced for efficiency
 * 
 * @author sfink
 */
public final class AssignEquation extends UnaryStatement implements VerboseAction {

  private final boolean DEBUG = false;

  AssignEquation(IVariable lhs, IVariable rhs) {
    super(lhs, rhs);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.AbstractEquation#getOperator()
   */
  @Override
  public AbstractOperator getOperator() {
    return PropagationCallGraphBuilder.assignOperator;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof AssignEquation) {
      AssignEquation other = (AssignEquation) o;
      return getLHS().equals(other.getLHS()) && getRightHandSide().equals(other.getRightHandSide());
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   */
  public void performVerboseAction() {
    if (DEBUG) {
      IntSetVariable lhs = (IntSetVariable) getLHS();
      IntSetVariable rhs = (IntSetVariable) getRightHandSide();
      System.err.println("LHS " + ((lhs.getValue() == null) ? "null" : lhs.getValue().getClass().toString()));
      System.err.println("RHS " + ((rhs.getValue() == null) ? "null" : rhs.getValue().getClass().toString()));
      if (lhs.getValue() instanceof MutableSharedBitVectorIntSet) {
        if (rhs.getValue() instanceof MutableSharedBitVectorIntSet) {
          MutableSharedBitVectorIntSet a = (MutableSharedBitVectorIntSet) lhs.getValue();
          MutableSharedBitVectorIntSet b = (MutableSharedBitVectorIntSet) rhs.getValue();
          System.err.println("Shared? " + MutableSharedBitVectorIntSet.sameSharedPart(a, b));
          IntSet diff = IntSetUtil.diff(a,b);
          System.err.println("Diff a/b" + diff.size() + " " + diff);
          diff = IntSetUtil.diff(b,a);
          System.err.println("Diff b/a" + diff.size() + " " + diff);
        }
      }
    }
  }
}
