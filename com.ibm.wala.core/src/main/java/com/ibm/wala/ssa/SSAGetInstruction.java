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

import com.ibm.wala.types.FieldReference;

/** SSA instruction that reads a field (i.e. getstatic or getfield). */
public abstract class SSAGetInstruction extends SSAFieldAccessInstruction {
  private final int result;

  protected SSAGetInstruction(int iindex, int result, int ref, FieldReference field) {
    super(iindex, field, ref);
    this.result = result;
  }

  protected SSAGetInstruction(int iindex, int result, FieldReference field) {
    super(iindex, field, -1);
    this.result = result;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    if (isStatic())
      return insts.GetInstruction(
          iIndex(), defs == null || defs.length == 0 ? result : defs[0], getDeclaredField());
    else
      return insts.GetInstruction(
          iIndex(),
          defs == null || defs.length == 0 ? result : defs[0],
          uses == null ? getRef() : uses[0],
          getDeclaredField());
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    if (isStatic()) {
      return getValueString(symbolTable, result) + " = getstatic " + getDeclaredField();
    } else {
      return getValueString(symbolTable, result)
          + " = getfield "
          + getDeclaredField()
          + ' '
          + getValueString(symbolTable, getRef());
    }
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor) */
  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitGet(this);
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

  /** @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses() */
  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int getNumberOfUses() {
    return isStatic() ? 0 : 1;
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getUse(int) */
  @Override
  public int getUse(int j) {
    assert j == 0 && getRef() != -1;
    return getRef();
  }

  @Override
  public int hashCode() {
    return result * 2371 + 6521;
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#isFallThrough() */
  @Override
  public boolean isFallThrough() {
    return true;
  }
}
