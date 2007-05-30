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
package com.ibm.wala.fixedpoint.impl;

import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * An operator of the form lhs = op (rhs)
 * 
 * @author Stephen Fink
 */
public abstract class UnaryOperator extends AbstractOperator implements FixedPointConstants {

  /**
   * Evaluate this equation, setting a new value for the left-hand side.
   * 
   * @return true if the lhs value changes. false otherwise.
   */
  public abstract byte evaluate(IVariable lhs, IVariable rhs);

  /**
   * Create an equation which uses this operator Override in subclasses for
   * efficiency.
   */
  public UnaryStatement makeEquation(IVariable lhs, IVariable rhs) {
    return new BasicUnaryStatement(lhs, this, rhs);
  }

  public boolean isIdentity() {
    return false;
  }

  @Override
  public byte evaluate(IVariable lhs, IVariable[] rhs) throws UnimplementedError {
    // this should never be called. Use the other, more efficient form.
    Assertions.UNREACHABLE();
    return 0;
  }
}
