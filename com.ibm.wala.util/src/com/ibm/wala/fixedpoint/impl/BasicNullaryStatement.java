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

import com.ibm.wala.fixpoint.IVariable;

/**
 * An implementation of NullaryStep that carries its operator explicitly
 */
@SuppressWarnings("rawtypes")
public class BasicNullaryStatement<T extends IVariable> extends NullaryStatement<T> {

  /**
   * The operator in the equation
   */
  private final NullaryOperator<T> operator;

  BasicNullaryStatement(T lhs, NullaryOperator<T> operator) {
    super(lhs);
    this.operator = operator;
  }

  /**
   * @return Returns the operator.
   */
  @Override
  public NullaryOperator<T> getOperator() {
    return operator;
  }
}
