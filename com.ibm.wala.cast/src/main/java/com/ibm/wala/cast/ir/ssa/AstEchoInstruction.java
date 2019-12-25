/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;

public class AstEchoInstruction extends AstConsumeInstruction {

  public AstEchoInstruction(int iindex, int[] rvals) {
    super(iindex, rvals);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstInstructionFactory) insts).EchoInstruction(iIndex(), uses == null ? rvals : uses);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    StringBuilder result = new StringBuilder("echo/print ");
    for (int rval : rvals) {
      result.append(getValueString(symbolTable, rval)).append(' ');
    }

    return result.toString();
  }

  @Override
  public void visit(IVisitor v) {
    ((AstInstructionVisitor) v).visitEcho(this);
  }
}
