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

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * A checkcast (dynamic type test) instruction. This instruction produces a new value number (like an assignment) if the check
 * succeeds.
 */
public abstract class SSACheckCastInstruction extends SSAInstruction {

  /**
   * A new value number def'fed by this instruction when the type check succeeds.
   */
  private final int result;

  /**
   * The value being checked by this instruction
   */
  private final int val;

  /**
   * The type which this instruction checks; the assignment succeeds if the val is a subtype of this type
   */
  private final TypeReference declaredResultType;

  /**
   * @param result A new value number def'fed by this instruction when the type check succeeds.
   * @param val The value being checked by this instruction
   * @param type The type which this instruction checks
   */
  protected SSACheckCastInstruction(int result, int val, TypeReference type) {
    super();
    this.result = result;
    this.val = val;
    this.declaredResultType = type;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    if (defs != null && defs.length == 0) {
      throw new IllegalArgumentException("(defs != null) and (defs.length == 0)");
    }
    if (uses != null && uses.length == 0) {
      throw new IllegalArgumentException("(uses != null) and (uses.length == 0)");
    }
    return insts.CheckCastInstruction(defs == null ? result : defs[0], uses == null ? val : uses[0], declaredResultType);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result) + " = checkcast " + declaredResultType.getName() + " "
        + getValueString(symbolTable, val);
  }

  /*
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * @throws IllegalArgumentException if v is null
   */
  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitCheckCast(this);
  }


  /* 
   * @see com.ibm.wala.ssa.SSAInstruction#hasDef()
   */
  @Override
  public boolean hasDef() {
    return true;
  }

  /**
   * @return A new value number def'fed by this instruction when the type check succeeds.
   */
  @Override
  public int getDef() {
    return result;
  }

  /* 
   * @see com.ibm.wala.ssa.SSAInstruction#getDef(int)
   */
  @Override
  public int getDef(int i) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(i == 0);
    }
    return result;
  }

  /*
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j == 0);
    return val;
  }

  public TypeReference getDeclaredResultType() {
    return declaredResultType;
  }

  public int getResult() {
    return result;
  }

  public int getVal() {
    return val;
  }

  @Override
  public int hashCode() {
    return result * 7529 + val;
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
    return true;
  }

  @Override
  public String toString() {
    return super.toString() + " " + declaredResultType;
  }

}
