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

import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.IVariable;

/**
 * An operator of the form lhs = op
 */
public abstract class NullaryOperator<T extends IVariable<T>> extends AbstractOperator<T> {

  @Override
  public byte evaluate(T lhs, T[] rhs) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
  /** 
   * Evaluate this equation, setting a new value for the
   * left-hand side. 
   * 
   * @return true if the lhs value changes. false otherwise.
   */
  public abstract byte evaluate(T lhs);
}
