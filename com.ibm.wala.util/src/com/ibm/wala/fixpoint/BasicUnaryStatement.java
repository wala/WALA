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
 * An implementation of UnaryStatement that carries its operator explicitly
 */
public class BasicUnaryStatement<T extends IVariable<T>> extends UnaryStatement<T> {

  private final UnaryOperator<T> operator;

  BasicUnaryStatement(T lhs, UnaryOperator<T> operator, T rhs) {
    super(lhs, rhs);
    this.operator = operator;
  }

  @Override
  public UnaryOperator<T> getOperator() {
    return operator;
  }
}
