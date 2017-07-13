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
 * operator for a step in an iterative solver
 * 
 * This is an abstract class and not an interface in order to force subclasses to re-implement equals(), hashCode(), and toString()
 */
public abstract class AbstractOperator<T extends IVariable<T>> implements FixedPointConstants {

  /**
   * Evaluate this equation, setting a new value for the left-hand side.
   * 
   * @return a code that indicates: 1) has the lhs value changed? 2) has this equation reached a fixed-point, in that we never have
   *         to evaluate the equation again, even if rhs operands change?
   */
  public abstract byte evaluate(T lhs, T[] rhs);

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract String toString();
}
