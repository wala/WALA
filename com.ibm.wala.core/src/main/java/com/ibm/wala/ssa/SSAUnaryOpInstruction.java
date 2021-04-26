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

import com.ibm.wala.shrike.shrikeBT.IUnaryOpInstruction;

/**
 * An SSA instruction for some unary operator.
 *
 * @see IUnaryOpInstruction for a list of operators
 */
public class SSAUnaryOpInstruction extends SSAAbstractUnaryInstruction {

  private final IUnaryOpInstruction.IOperator operator;

  public SSAUnaryOpInstruction(
      int iindex, IUnaryOpInstruction.IOperator operator, int result, int val) {
    super(iindex, result, val);
    this.operator = operator;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses)
      throws IllegalArgumentException {
    if (uses != null && uses.length == 0) {
      throw new IllegalArgumentException("(uses != null) and (uses.length == 0)");
    }
    return insts.UnaryOpInstruction(
        iIndex(),
        operator,
        defs == null || defs.length == 0 ? result : defs[0],
        uses == null ? val : uses[0]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result)
        + " = "
        + operator
        + ' '
        + getValueString(symbolTable, val);
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor) */
  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitUnaryOp(this);
  }

  public IUnaryOpInstruction.IOperator getOpcode() {
    return operator;
  }
}
