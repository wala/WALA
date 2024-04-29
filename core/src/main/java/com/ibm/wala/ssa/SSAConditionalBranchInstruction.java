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

import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction.IOperator;
import com.ibm.wala.types.TypeReference;

/** A conditional branch instruction, which tests two values according to some {@link IOperator}. */
public class SSAConditionalBranchInstruction extends SSAInstruction {
  private final IConditionalBranchInstruction.IOperator operator;

  private final int val1;

  private final int val2;

  private final TypeReference type;

  private final int target;

  public SSAConditionalBranchInstruction(
      int iindex,
      IConditionalBranchInstruction.IOperator operator,
      TypeReference type,
      int val1,
      int val2,
      int target)
      throws IllegalArgumentException {
    super(iindex);
    this.target = target;
    this.operator = operator;
    this.val1 = val1;
    this.val2 = val2;
    this.type = type;
    if (val1 <= 0) {
      throw new IllegalArgumentException("Invalid val1: " + val1);
    }
    if (val2 <= 0) {
      throw new IllegalArgumentException("Invalid val2: " + val2);
    }
  }

  public int getTarget() {
    return target;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses)
      throws IllegalArgumentException {
    if (uses != null && uses.length < 2) {
      throw new IllegalArgumentException("(uses != null) and (uses.length < 2)");
    }
    return insts.ConditionalBranchInstruction(
        iIndex(),
        operator,
        type,
        uses == null ? val1 : uses[0],
        uses == null ? val2 : uses[1],
        target);
  }

  public IConditionalBranchInstruction.IOperator getOperator() {
    return operator;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "conditional branch("
        + operator
        + ", to iindex="
        + target
        + ") "
        + getValueString(symbolTable, val1)
        + ','
        + getValueString(symbolTable, val2);
  }

  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitConditionalBranch(this);
  }

  @Override
  public int getNumberOfUses() {
    return 2;
  }

  @Override
  public int getUse(int j) {
    assert j <= 1;
    return (j == 0) ? val1 : val2;
  }

  public TypeReference getType() {
    return type;
  }

  public boolean isObjectComparison() {
    return type.isReferenceType();
  }

  public boolean isIntegerComparison() {
    return type == TypeReference.Int;
  }

  @Override
  public int hashCode() {
    return 7151 * val1 + val2;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
