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
 * Represents a single step in an iterative solver
 * 
 * @author Stephen Fink
 * @author Julian Dolby
 */
public class GeneralStatement extends AbstractStatement {

  protected final IVariable lhs;
  protected final IVariable[] rhs;

  private final int hashCode;

  private final AbstractOperator operator;

  /**
   * Evaluate this equation, setting a new value for the left-hand side.
   * 
   * @return true if the lhs value changed. false otherwise
   */
  public byte evaluate() {
    return operator.evaluate(lhs, rhs);
  }

  /**
   * Return the left-hand side of this equation.
   * 
   * @return the lattice cell this equation computes
   */
  public IVariable getLHS() {
    return lhs;
  }

  /**
   * Does this equation contain an appearance of a given cell?
   * 
   * Note: this uses reference equality, assuming that the variables are
   * canonical! This is fragile. TODO: Address it perhaps, but be careful not to
   * sacrifice efficiency.
   * 
   * @param cell
   *          the cell in question
   * @return true or false
   */
  public boolean hasVariable(IVariable cell) {
    if (lhs == cell) {
      return true;
    }
    for (int i = 0; i < rhs.length; i++) {
      if (rhs[i] == cell)
        return true;
    }
    return false;
  }

  /**
   * Constructor for case of zero operands on the right-hand side.
   * 
   * @param lhs
   *          the lattice cell set by this equation
   * @param operator
   *          the equation operator
   */
  public GeneralStatement(IVariable lhs, AbstractOperator operator) {
    super();
    this.operator = operator;
    this.lhs = lhs;
    this.rhs = null;
    this.hashCode = makeHashCode();
  }

  /**
   * Constructor for case of two operands on the right-hand side.
   * 
   * @param lhs
   *          the lattice cell set by this equation
   * @param operator
   *          the equation operator
   * @param op1
   *          the first operand on the rhs
   * @param op2
   *          the second operand on the rhs
   */
  public GeneralStatement(IVariable lhs, AbstractOperator operator, IVariable op1, IVariable op2) {
    super();
    this.operator = operator;
    this.lhs = lhs;
    rhs = new IVariable[2];
    rhs[0] = op1;
    rhs[1] = op2;
    this.hashCode = makeHashCode();
  }

  /**
   * Constructor for case of three operands on the right-hand side.
   * 
   * @param lhs
   *          the lattice cell set by this equation
   * @param operator
   *          the equation operator
   * @param op1
   *          the first operand on the rhs
   * @param op2
   *          the second operand on the rhs
   * @param op3
   *          the third operand on the rhs
   */
  public GeneralStatement(IVariable lhs, AbstractOperator operator, IVariable op1, IVariable op2, IVariable op3) {
    super();
    this.operator = operator;
    rhs = new IVariable[3];
    this.lhs = lhs;
    rhs[0] = op1;
    rhs[1] = op2;
    rhs[2] = op3;
    this.hashCode = makeHashCode();
  }

  /**
   * Constructor for case of more than three operands on the right-hand side.
   * 
   * @param lhs
   *          the lattice cell set by this equation
   * @param operator
   *          the equation operator
   * @param rhs
   *          the operands of the right-hand side in order
   */
  public GeneralStatement(IVariable lhs, AbstractOperator operator, IVariable[] rhs) {
    super();
    this.operator = operator;
    this.lhs = lhs;
    this.rhs = (IVariable[]) rhs.clone();
    this.hashCode = makeHashCode();
  }

  /**
   * TODO: use a better hash code?
   */
  private final static int[] primes = { 331, 337, 347, 1277 };

  private int makeHashCode() {
    int result = operator.hashCode();
    if (lhs != null) result += lhs.hashCode() * primes[0];
    for (int i = 0; i < Math.min(rhs.length, 2); i++) {
      if (rhs[i] != null) {
        result += primes[i + 1] * rhs[i].hashCode();
      }
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return hashCode;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if (getClass().equals(o.getClass())) {
      GeneralStatement other = (GeneralStatement) o;
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

  /**
   * @return Returns the operator.
   */
  public AbstractOperator getOperator() {
    return operator;
  }

  /* (non-Javadoc)
   */
  public IVariable[] getRHS() {
    return rhs;
  }
}
