/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;

/**
 * A simple assignment statement. Only appears in the IR before SSA conversion, and temporarily when needed to undo copy propagation
 * during processing of new lexical definitions and uses.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 * 
 */
public class AssignInstruction extends SSAUnaryOpInstruction {

  /**
   * create the assignment v_result := v_val
   * 
   * @param result
   * @param val
   */
  public AssignInstruction(int iindex, int result, int val) {
    super(iindex, null, result, val);
    assert result != val;
    assert result != -1;
    assert val != -1;
  }

  /*
   * @see com.ibm.wala.ssa.SSAInstruction#copyForSSA(int[], int[])
   */
  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstInstructionFactory) insts)
        .AssignInstruction(iindex, defs == null ? getDef(0) : defs[0], uses == null ? getUse(0) : uses[0]);
  }

  /*
   * @see com.ibm.wala.ssa.SSAInstruction#toString(com.ibm.wala.ssa.SymbolTable, com.ibm.wala.ssa.ValueDecorator)
   */
  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result) + " := " + getValueString(symbolTable, val);
  }

  /*
   * @see com.ibm.wala.ssa.SSAInstruction#visit(com.ibm.wala.ssa.SSAInstruction.Visitor)
   */
  @Override
  public void visit(IVisitor v) {
    ((AstPreInstructionVisitor) v).visitAssign(this);
  }

  public int getVal() {
    return getUse(0);
  }
}
