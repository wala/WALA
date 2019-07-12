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

import com.ibm.wala.types.TypeReference;

/** SSA instruction representing an array store. */
public abstract class SSAArrayStoreInstruction extends SSAArrayReferenceInstruction {

  private final int value;

  protected SSAArrayStoreInstruction(
      int iindex, int arrayref, int index, int value, TypeReference elementType) {
    super(iindex, arrayref, index, elementType);
    this.value = value;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    if (uses != null && uses.length < 3) {
      throw new IllegalArgumentException("uses.length < 3");
    }
    return insts.ArrayStoreInstruction(
        iIndex(),
        uses == null ? getArrayRef() : uses[0],
        uses == null ? getIndex() : uses[1],
        uses == null ? value : uses[2],
        getElementType());
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "arraystore "
        + getValueString(symbolTable, getArrayRef())
        + '['
        + getValueString(symbolTable, getIndex())
        + "] = "
        + getValueString(symbolTable, value);
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
    v.visitArrayStore(this);
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses() */
  @Override
  public int getNumberOfUses() {
    return 3;
  }

  @Override
  public int getNumberOfDefs() {
    return 0;
  }

  public int getValue() {
    return value;
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getUse(int) */
  @Override
  public int getUse(int j) {
    if (j == 2) return value;
    else return super.getUse(j);
  }

  @Override
  public int hashCode() {
    return 6311 * value ^ 2371 * getArrayRef() + getIndex();
  }
}
