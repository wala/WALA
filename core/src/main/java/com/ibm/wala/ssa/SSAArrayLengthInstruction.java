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

/** SSA instruction representing v_x := arraylength v_y */
public abstract class SSAArrayLengthInstruction extends SSAInstruction {
  private final int result;

  private final int arrayref;

  protected SSAArrayLengthInstruction(int iindex, int result, int arrayref) {
    super(iindex);
    this.result = result;
    this.arrayref = arrayref;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses)
      throws IllegalArgumentException {
    if (defs != null && defs.length != 1) {
      throw new IllegalArgumentException();
    }
    if (uses != null && uses.length != 1) {
      throw new IllegalArgumentException();
    }
    return insts.ArrayLengthInstruction(
        iIndex(), defs == null ? result : defs[0], uses == null ? arrayref : uses[0]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result)
        + " = arraylength "
        + getValueString(symbolTable, arrayref);
  }

  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitArrayLength(this);
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef(int i) {
    if (i != 0) {
      throw new IllegalArgumentException("invalid i " + i);
    }
    return result;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  public int getArrayRef() {
    return arrayref;
  }

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getUse(int j) {
    if (j != 0) {
      throw new IllegalArgumentException("invalid j: " + j);
    }
    return arrayref;
  }

  @Override
  public int hashCode() {
    return arrayref * 7573 + result * 563;
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
