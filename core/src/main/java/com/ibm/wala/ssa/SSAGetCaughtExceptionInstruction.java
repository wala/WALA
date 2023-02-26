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

/**
 * A "catch" instruction, inserted at the head of a catch block, which assigns a pending exception
 * object to a local variable.
 *
 * <p>In SSA {@link IR}s, these instructions do <em>not</em> appear in the normal instruction array
 * returned by IR.getInstructions(); instead these instructions live in {@link ISSABasicBlock}.
 */
public class SSAGetCaughtExceptionInstruction extends SSAInstruction {
  private final int exceptionValueNumber;

  private final int bbNumber;

  public SSAGetCaughtExceptionInstruction(int iindex, int bbNumber, int exceptionValueNumber) {
    super(iindex);
    this.exceptionValueNumber = exceptionValueNumber;
    this.bbNumber = bbNumber;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    assert defs == null || defs.length == 1;
    return insts.GetCaughtExceptionInstruction(
        iIndex(), bbNumber, defs == null ? exceptionValueNumber : defs[0]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, exceptionValueNumber) + " = getCaughtException ";
  }

  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitGetCaughtException(this);
  }

  /**
   * Returns the result.
   *
   * @return int
   */
  public int getException() {
    return exceptionValueNumber;
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getDef() */
  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return exceptionValueNumber;
  }

  @Override
  public int getDef(int i) {
    assert i == 0;
    return exceptionValueNumber;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  public int getBasicBlockNumber() {
    return bbNumber;
  }

  @Override
  public int hashCode() {
    return 2243 * exceptionValueNumber;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
