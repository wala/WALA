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
import com.ibm.wala.fixpoint.AbstractStatement;
import com.ibm.wala.fixpoint.IVariable;

/**
 * Represents a single step in an iterative solver
 */
public abstract class GeneralStatement<T extends IVariable<T>> extends AbstractStatement<T, AbstractOperator<T>> {

  protected final T lhs;

  protected final T[] rhs;

  private final int hashCode;

  private final AbstractOperator<T> operator;

  /**
   * Evaluate this equation, setting a new value for the left-hand side.
   * 
   * @return true if the lhs value changed. false otherwise
   */
  @Override
  public byte evaluate() {
    return operator.evaluate(lhs, rhs);
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
   * 
   * Note: this uses reference equality, assuming that the variables are canonical! This is fragile. TODO: Address it perhaps, but
   * be careful not to sacrifice efficiency.
   * 
   * @param cell the cell in question
   * @return true or false
   */
  @Override
  public boolean hasVariable(T cell) {
    if (lhs == cell) {
      return true;
    }
    for (T rh : rhs) {
      if (rh == cell)
        return true;
    }
    return false;
  }

  /**
   * Constructor for case of zero operands on the right-hand side.
   * 
   * @param lhs the lattice cell set by this equation
   * @param operator the equation operator
   */
  public GeneralStatement(T lhs, AbstractOperator<T> operator) {
    super();
    if (operator == null) {
      throw new IllegalArgumentException("null operator");
    }
    this.operator = operator;
    this.lhs = lhs;
    this.rhs = null;
    this.hashCode = makeHashCode();
  }

  /**
   * Constructor for case of two operands on the right-hand side.
   * 
   * @param lhs the lattice cell set by this equation
   * @param operator the equation operator
   * @param op1 the first operand on the rhs
   * @param op2 the second operand on the rhs
   */
  public GeneralStatement(T lhs, AbstractOperator<T> operator, T op1, T op2) {
    super();
    if (operator == null) {
      throw new IllegalArgumentException("null operator");
    }
    this.operator = operator;
    this.lhs = lhs;
    rhs = makeRHS(2);
    rhs[0] = op1;
    rhs[1] = op2;
    this.hashCode = makeHashCode();
  }

  /**
   * Constructor for case of three operands on the right-hand side.
   * 
   * @param lhs the lattice cell set by this equation
   * @param operator the equation operator
   * @param op1 the first operand on the rhs
   * @param op2 the second operand on the rhs
   * @param op3 the third operand on the rhs
   */
  public GeneralStatement(T lhs, AbstractOperator<T> operator, T op1, T op2, T op3) {
    super();
    if (operator == null) {
      throw new IllegalArgumentException("null operator");
    }
    this.operator = operator;
    rhs = makeRHS(3);
    this.lhs = lhs;
    rhs[0] = op1;
    rhs[1] = op2;
    rhs[2] = op3;
    this.hashCode = makeHashCode();
  }

  /**
   * Constructor for case of more than three operands on the right-hand side.
   * 
   * @param lhs the lattice cell set by this equation
   * @param operator the equation operator
   * @param rhs the operands of the right-hand side in order
   * @throws IllegalArgumentException if rhs is null
   */
  public GeneralStatement(T lhs, AbstractOperator<T> operator, T[] rhs) {
    super();
    if (operator ==  null) {
      throw new IllegalArgumentException("null operator");
    }
    if (rhs == null) {
      throw new IllegalArgumentException("rhs is null");
    }
    this.operator = operator;
    this.lhs = lhs;
    this.rhs = rhs.clone();
    this.hashCode = makeHashCode();
  }

  /**
   * TODO: use a better hash code?
   */
  private final static int[] primes = { 331, 337, 347, 1277 };

  private int makeHashCode() {
    int result = operator.hashCode();
    if (lhs != null)
      result += lhs.hashCode() * primes[0];
    for (int i = 0; i < Math.min(rhs.length, 2); i++) {
      if (rhs[i] != null) {
        result += primes[i + 1] * rhs[i].hashCode();
      }
    }
    return result;
  }

  protected abstract T[] makeRHS(int size);
  
  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (getClass().equals(o.getClass())) {
      GeneralStatement<?> other = (GeneralStatement<?>) o;
      if (hashCode == other.hashCode) {
        if (lhs == null || other.lhs == null) {
          if (other.lhs != lhs) {
            return false;
          }
        } else if (!lhs.equals(other.lhs)) {
          return false;
        }
        if (operator.equals(other.operator) && rhs.length == other.rhs.length) {
          for (int i = 0; i < rhs.length; i++) {
            if (rhs[i] == null || other.rhs[i] == null) {
              if (other.rhs[i] != rhs[i]) {
                return false;
              }
            } else if (!rhs[i].equals(other.rhs[i])) {
              return false;
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public AbstractOperator<T> getOperator() {
    return operator;
  }

  @Override
  public T[] getRHS() {
    return rhs;
  }
}
