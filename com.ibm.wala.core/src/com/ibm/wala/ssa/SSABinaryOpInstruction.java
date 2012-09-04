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
package com.ibm.wala.ssa;

import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;

public abstract class SSABinaryOpInstruction extends SSAInstruction {

  private final int result;

  private final int val1;

  private final int val2;

  private final IBinaryOpInstruction.IOperator operator;

  /**
   * Might this instruction represent integer arithmetic?
   */
  private final boolean mayBeInteger;

  protected SSABinaryOpInstruction(IBinaryOpInstruction.IOperator operator, int result, int val1, int val2, boolean mayBeInteger) {
    super();
    this.result = result;
    this.val1 = val1;
    this.val2 = val2;
    this.operator = operator;
    this.mayBeInteger = mayBeInteger;
    if (val1 <= 0) {
      throw new IllegalArgumentException("illegal val1: " + val1);
    }
    if (val2 <= 0) {
      throw new IllegalArgumentException("illegal val2: " + val2);
    }
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result) + " = binaryop(" + operator + ") " + getValueString(symbolTable, val1) + " , "
        + getValueString(symbolTable, val2);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitBinaryOp(this);
  }

  /**
   * Ugh. clean up shrike operator stuff.
   */
  public IBinaryOpInstruction.IOperator getOperator() {
    return operator;
  }

  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public int getDef(int i) {
    assert i == 0;
    return result;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int getNumberOfUses() {
    return 2;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    assert j <= 1;
    return (j == 0) ? val1 : val2;
  }

  @Override
  public int hashCode() {
    return 6311 * result ^ 2371 * val1 + val2;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isPEI()
   */
  @Override
  public boolean isPEI() {
    return mayBeInteger && (operator == BinaryOpInstruction.Operator.DIV || operator == BinaryOpInstruction.Operator.REM);
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

  public boolean mayBeIntegerOp() {
    return mayBeInteger;
  }
}
