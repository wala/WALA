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
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.ssa.SSAAbstractUnaryInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;

/**
 * Non-deterministically assigns some object in the prototype chain of val (or val itself) to
 * result.
 */
public class PrototypeLookup extends SSAAbstractUnaryInstruction {

  public PrototypeLookup(int iindex, int result, int val) {
    super(iindex, result, val);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((JSInstructionFactory) insts)
        .PrototypeLookup(
            iIndex(), (defs != null ? defs[0] : getDef(0)), (uses != null ? uses[0] : getUse(0)));
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, getDef(0))
        + " = prototype_values("
        + getValueString(symbolTable, getUse(0))
        + ')';
  }

  @Override
  public void visit(IVisitor v) {
    ((JSInstructionVisitor) v).visitPrototypeLookup(this);
  }
}
