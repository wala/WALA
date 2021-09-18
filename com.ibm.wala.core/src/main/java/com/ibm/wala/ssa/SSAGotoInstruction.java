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

/** Unconditional branch instruction for SSA form. */
public class SSAGotoInstruction extends SSAInstruction {
  private final int target;

  public SSAGotoInstruction(int iindex, int target) {
    super(iindex);
    this.target = target;
  }

  /**
   * getTarget returns the IIndex for the Goto-target. Not to be confused with the array-index in
   * InducedCFG.getStatements()
   */
  public int getTarget() {
    return this.target;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return insts.GotoInstruction(iIndex(), target);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "goto (from iindex= " + this.iIndex() + " to iindex = " + this.target + ')';
  }

  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitGoto(this);
  }

  @Override
  public int hashCode() {
    return 1409 + 17 * target;
  }

  @Override
  public boolean isFallThrough() {
    return false;
  }
}
