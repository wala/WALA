/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ssa;

import com.ibm.wala.shrike.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;

public abstract class SSABinaryOpInstruction extends SSAAbstractBinaryInstruction {

  private final IBinaryOpInstruction.IOperator operator;

  /** Might this instruction represent integer arithmetic? */
  private final boolean mayBeInteger;

  protected SSABinaryOpInstruction(
      int iindex,
      IBinaryOpInstruction.IOperator operator,
      int result,
      int val1,
      int val2,
      boolean mayBeInteger) {
    super(iindex, result, val1, val2);
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
    return getValueString(symbolTable, result)
        + " = binaryop("
        + operator
        + ") "
        + getValueString(symbolTable, val1)
        + " , "
        + getValueString(symbolTable, val2);
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor) */
  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitBinaryOp(this);
  }

  /** Ugh. clean up shrike operator stuff. */
  public IBinaryOpInstruction.IOperator getOperator() {
    return operator;
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#isPEI() */
  @Override
  public boolean isPEI() {
    return mayBeInteger
        && (operator == BinaryOpInstruction.Operator.DIV
            || operator == BinaryOpInstruction.Operator.REM);
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#isFallThrough() */
  @Override
  public boolean isFallThrough() {
    return true;
  }

  public boolean mayBeIntegerOp() {
    return mayBeInteger;
  }
}
