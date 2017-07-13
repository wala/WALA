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

import com.ibm.wala.fixpoint.AbstractStatement;
import com.ibm.wala.fixpoint.IVariable;

/**
 * Represents a single step, restricted to a nullary
 * operator.
 */
public abstract class NullaryStatement<T extends IVariable<T>> extends AbstractStatement<T, NullaryOperator<T>> {

  /**
   * The operands
   */
  final protected T lhs;

  /** 
   * Evaluate this equation, setting a new value for the
   * left-hand side. 
   * 
   * @return true if the lhs value changed. false otherwise
   */
  @Override
  public byte evaluate() {
    NullaryOperator<T> op = getOperator();
    return op.evaluate(lhs);
  }

  /** 
   * Return the left-hand side of this equation.
   * 
   * @return the lattice cell this equation computes
   */
  @Override
  public T getLHS() {
    return lhs;
  }

  /** 
   * Does this equation contain an appearance of a given cell?
   * @param cell the cell in question
   * @return true or false
   */
  @Override
  public boolean hasVariable(T cell) {
    return lhs == cell;
  }

  /** 
   * Constructor for case of one operand on the right-hand side.
   *
   * @param lhs the lattice cell set by this equation
   */
  protected NullaryStatement(T lhs) {
    super();
    this.lhs = lhs;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof NullaryStatement) {
      NullaryStatement<?> other = (NullaryStatement<?>) o;

      if (!getOperator().equals(other.getOperator())) {
        return false;
      }
      if (lhs == null) {
        if (other.lhs != null) {
          return false;
        }
      } else {
        if (other.lhs == null) {
          return false;
        }
        if (!lhs.equals(other.lhs)) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int result = getOperator().hashCode() * 1381;
    if (lhs != null) {
      result += 1399 * lhs.hashCode();
    }
    return result;
  }
  
  @Override
  public T[] getRHS() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}
