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

/** An instruction which converts a value of one primitive type into another primitive type. */
public abstract class SSAConversionInstruction extends SSAInstruction {
  private final int result;

  private final int val;

  private final TypeReference fromType;

  private final TypeReference toType;

  protected SSAConversionInstruction(
      int iindex, int result, int val, TypeReference fromType, TypeReference toType) {
    super(iindex);
    this.result = result;
    this.val = val;
    this.fromType = fromType;
    this.toType = toType;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result)
        + " = conversion("
        + toType.getName()
        + ") "
        + getValueString(symbolTable, val);
  }

  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitConversion(this);
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

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  public TypeReference getToType() {
    return toType;
  }

  public TypeReference getFromType() {
    return fromType;
  }

  @Override
  public int getUse(int j) {
    assert j == 0;
    return val;
  }

  @Override
  public int hashCode() {
    return 6311 * result ^ val;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
