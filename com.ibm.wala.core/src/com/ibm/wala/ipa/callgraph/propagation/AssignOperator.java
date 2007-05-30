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

import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixedpoint.impl.UnaryStatement;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.util.debug.Trace;

/**
 * Corresponds to: "is a superset of". Used for assignment.
 * 
 * Unary op: <lhs>:= Assign( <rhs>)
 * 
 * (Technically, it's a binary op, since it includes lhs as an implicit input;
 * this allows it to compose with other ops that define the same lhs, so long as
 * they're all Assign ops)
 */
class AssignOperator extends UnaryOperator implements IPointerOperator {

  @Override
  public UnaryStatement makeEquation(IVariable lhs, IVariable rhs) {
    return new AssignEquation(lhs, rhs);
  }

  @Override
  public byte evaluate(IVariable lhs, IVariable rhs) {

    PointsToSetVariable L = (PointsToSetVariable) lhs;
    PointsToSetVariable R = (PointsToSetVariable) rhs;

    boolean debug = false;
    if (PropagationCallGraphBuilder.DEBUG_ASSIGN) {
      String S = "EVAL Assign " + L.getPointerKey() + " " + R.getPointerKey();
      S = S + "\nEVAL " + lhs + " " + rhs;
      debug = Trace.guardedPrintln(S, PropagationCallGraphBuilder.DEBUG_METHOD_SUBSTRING);
    }
    boolean changed = L.addAll(R);
    if (PropagationCallGraphBuilder.DEBUG_ASSIGN) {
      if (debug) {
        Trace.println("RESULT " + L + (changed ? " (changed)" : ""));
      }
    }

    if (PropagationCallGraphBuilder.DEBUG_TRACK_INSTANCE) {
      if (changed) {
        if (L.contains(PropagationCallGraphBuilder.DEBUG_INSTANCE_KEY)
            && R.contains(PropagationCallGraphBuilder.DEBUG_INSTANCE_KEY)) {
          Trace.println("Assign: FLOW FROM " + R.getPointerKey() + " TO " + L.getPointerKey());
        }
      }
    }

    return changed ? CHANGED : NOT_CHANGED;
  }

  @Override
  public String toString() {
    return "Assign";
  }

  @Override
  public int hashCode() {
    return 9883;
  }

  @Override
  public final boolean equals(Object o) {
    // this is a singleton
    return (this == o);
  }

  /* 
   * @see com.ibm.wala.ipa.callgraph.propagation.IPointerOperator#isComplex()
   */
  public boolean isComplex() {
    return false;
  }
}