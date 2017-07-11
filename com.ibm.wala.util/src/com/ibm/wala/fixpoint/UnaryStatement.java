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
 * Represents a single step, restricted to a unary operator.
 */
public abstract class UnaryStatement<T extends IVariable<T>> extends AbstractStatement<T, UnaryOperator<T>> {

  /**
   * The operands
   */
  final protected T lhs;
  final protected T rhs;

  /** 
   * Evaluate this equation, setting a new value for the
   * left-hand side. 
   * 
   * @return true if the lhs value changed. false otherwise
   */
  @Override
  public byte evaluate() {
    UnaryOperator<T> op = getOperator();
    return op.evaluate(lhs, rhs);
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
   * @return the right-hand side of this equation.
   */
  public T getRightHandSide() {
    return rhs;
  }

  /** 
   * Return the operands in this equation.
   */
  public IVariable<T>[] getOperands() {
    @SuppressWarnings("unchecked")
    IVariable<T>[] result = new IVariable[2];
    result[0] = lhs;
    result[1] = rhs;
    return result;
  }

  /** 
   * Does this equation contain an appearance of a given cell?
   * @param cell the cell in question
   * @return true or false
   */
  @Override
  public boolean hasVariable(T cell) {
    if (lhs == cell)
      return true;
    if (rhs == cell)
      return true;
    return false;
  }

  /** 
   * Return a string representation of this object 
   * @return a string representation of this object 
   */
  @Override
  public String toString() {
    String result;
    if (lhs == null) {
      result = "null lhs";
    } else {
      result = lhs.toString();
    }
    result = result + " " + getOperator() + " " + rhs;
    return result;
  }

  /** 
   * Constructor for case of one operand on the right-hand side.
   *
   * @param lhs the lattice cell set by this equation
   * @param rhs the first operand on the rhs
   */
  protected UnaryStatement(T lhs, T rhs) {
    super();
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof UnaryStatement) {
      UnaryStatement<?> other = (UnaryStatement<?>) o;

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
      if (rhs == null) {
        if (other.rhs != null) {
          return false;
        }
      } else {
        if (other.rhs == null) {
          return false;
        }
        if (!rhs.equals(other.rhs)) {
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
    if (rhs != null) {
      result += 1409 * rhs.hashCode();
    }
    return result;
  }
  
  @Override
  public T[] getRHS() throws UnsupportedOperationException {
    // This should never be called ...use the more efficient getRightHandSide instead
    throw new UnsupportedOperationException();
  }
}
