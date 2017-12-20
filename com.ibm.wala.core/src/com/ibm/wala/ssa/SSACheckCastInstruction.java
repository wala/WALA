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

/**
 * A checkcast (dynamic type test) instruction. This instruction produces a new value number (like an assignment) if the check
 * succeeds.
 * 
 * Note that this instruction generalizes the meaning of checkcast in Java since it supports
 * multiple types for which to check.  The meaning is that the case succeeds if the object
 * is of any of the desired types.
 *  
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
   * The types for which this instruction checks; the assignment succeeds if the val is a subtype of one of these types
   */
  private final TypeReference[] declaredResultTypes;

  /**
   * whether the type test throws an exception
   */
  private final boolean isPEI;
  
  /**
   * @param result A new value number def'fed by this instruction when the type check succeeds.
   * @param val The value being checked by this instruction
   * @param types The types which this instruction checks
   */
  protected SSACheckCastInstruction(int iindex, int result, int val, TypeReference[] types, boolean isPEI) {
    super(iindex);
    assert val != -1;
    this.result = result;
    this.val = val;
    this.declaredResultTypes = types;
    this.isPEI = isPEI;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    if (defs != null && defs.length == 0) {
      throw new IllegalArgumentException("(defs != null) and (defs.length == 0)");
    }
    if (uses != null && uses.length == 0) {
      throw new IllegalArgumentException("(uses != null) and (uses.length == 0)");
    }
    return insts.CheckCastInstruction(iindex, defs == null ? result : defs[0], uses == null ? val : uses[0], declaredResultTypes, isPEI);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    String v = getValueString(symbolTable, result) + " = checkcast";
    for (TypeReference t : declaredResultTypes) {
        v = v + " " + t;
    }
    return v + getValueString(symbolTable, val);
  }

  /*
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * 
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
    assert i == 0;
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
    assert j == 0;
    return val;
  }

  /**
   * @deprecated the system now supports multiple types, so this
   * accessor will not work for all languages.
   */
  @Deprecated
  public TypeReference getDeclaredResultType() {
    assert declaredResultTypes.length == 1;
    return declaredResultTypes[0];
  }

  public TypeReference[] getDeclaredResultTypes() {
    return declaredResultTypes;
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
    return isPEI;
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
    String s = super.toString();
    for (TypeReference t : declaredResultTypes) {
      s = s + " " + t;
    }
    return s;
  }

}
