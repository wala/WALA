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
 * A return instruction.
 */
public class SSAReturnInstruction extends SSAInstruction {

  /**
   * value number of the result. By convention result == -1 means returns void.
   */
  private final int result;

  private final boolean isPrimitive;

  public SSAReturnInstruction(int result, boolean isPrimitive) {
    super();
    this.result = result;
    this.isPrimitive = isPrimitive;
  }

  public SSAReturnInstruction() {
    super();
    this.result = -1;
    this.isPrimitive = false;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    if (result == -1)
      return insts.ReturnInstruction();
    else {
      if (uses != null && uses.length != 1) {
        throw new IllegalArgumentException("invalid uses.  must have exactly one use.");
      }
      return insts.ReturnInstruction(uses == null ? result : uses[0], isPrimitive);
    }
  }

  @Override
  public String toString(SymbolTable table) {
    if (result == -1) {
      return "return";
    } else {
      return "return " + getValueString(table, result);
    }
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * @throws IllegalArgumentException if v is null
   */
  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitReturn(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfUses() {
    return (result == -1) ? 0 : 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    if (j != 0) {
      throw new IllegalArgumentException("illegal j: " + j);
    }
    return result;
  }

  /**
   * @return true iff this return instruction returns a primitive value
   */
  public boolean returnsPrimitiveType() {
    return isPrimitive;
  }

  public int getResult() {
    return result;
  }

  public boolean returnsVoid() {
    return result == -1;
  }

  @Override
  public int hashCode() {
    return result * 8933;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return false;
  }

 }
