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

/**
 * Unconditional branch instruction for SSA form.
 */
public class SSAGotoInstruction extends SSAInstruction {

  public SSAGotoInstruction() {
    super();
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return insts.GotoInstruction();
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "goto";
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * @throws IllegalArgumentException
   *           if v is null
   */
  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitGoto(this);
  }

  @Override
  public int hashCode() {
    return 1409; // XXX weak!
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return false;
  }
}
