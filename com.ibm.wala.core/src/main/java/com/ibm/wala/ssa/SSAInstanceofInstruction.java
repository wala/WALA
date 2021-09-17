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

/** A dynamic type test (instanceof) instruction. */
public class SSAInstanceofInstruction extends SSAInstruction {
  private final int result;

  private final int ref;

  private final TypeReference checkedType;

  public SSAInstanceofInstruction(int iindex, int result, int ref, TypeReference checkedType) {
    super(iindex);
    this.result = result;
    this.ref = ref;
    this.checkedType = checkedType;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    if (defs != null && defs.length == 0) {
      throw new IllegalArgumentException("defs.length == 0");
    }
    if (uses != null && uses.length == 0) {
      throw new IllegalArgumentException("uses.length == 0");
    }
    return insts.InstanceofInstruction(
        iIndex(),
        defs == null || defs.length == 0 ? result : defs[0],
        uses == null ? ref : uses[0],
        checkedType);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result)
        + " = instanceof "
        + getValueString(symbolTable, ref)
        + ' '
        + checkedType;
  }

  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitInstanceof(this);
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getDef() */
  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public int getDef(int i) {
    assert i == 0;
    return result;
  }

  public TypeReference getCheckedType() {
    return checkedType;
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses() */
  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getUse(int j) {
    assert j == 0;
    return ref;
  }

  @Override
  public int hashCode() {
    return ref * 677 ^ result * 3803;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  public int getRef() {
    return ref;
  }
}
