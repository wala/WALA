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
 * An instruction which unconditionally throws an exception
 */
public abstract class SSAAbstractThrowInstruction extends SSAInstruction {
  private final int exception;

  public SSAAbstractThrowInstruction(int exception) {
    super();
    //assert exception > 0;
    this.exception = exception;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "throw " + getValueString(symbolTable, exception);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfUses() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    if (j != 0) {
      throw new IllegalArgumentException("j must be 0");
    }
    return exception;
  }

  @Override
  public int hashCode() {
    return 7529 + exception * 823;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isPEI()
   */
  @Override
  public boolean isPEI() {
    return true;
  }

  /* 
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return false;
  }

  /**
   * @return value number of the thrown exception object.
   */
  public int getException() {
    return exception;
  }

}
