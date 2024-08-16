/*
 * Copyright (c) 2024 IBM Corporation.
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

/**
 * The Expression variant of Unspecified Instruction behaves as its parent, and assigns the result
 * of computing the opaque payload to a value.
 *
 * @param <T> The type of the payload.
 */
public class SSAUnspecifiedExprInstruction<T> extends SSAUnspecifiedInstruction<T> {

  private final int result;
  private final TypeReference type;

  /**
   * Create a new Uninterpreted Expression defining result as some un-parsed payload.
   *
   * @param iindex the instruction index
   * @param result the expression result's value number
   * @param resultType the type of the result
   * @param payload the payload to be placed in a CAstPrimitive node
   */
  public SSAUnspecifiedExprInstruction(
      int iindex, int result, TypeReference resultType, T payload) {
    super(iindex, payload);
    this.result = result;
    this.type = resultType;
  }

  public TypeReference getResultType() {
    return type;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses)
      throws IllegalArgumentException {
    assert (uses == null) : "Expected no uses in " + this.getClass().getSimpleName();
    return insts.UnspecifiedExprInstruction(
        iIndex(), defs == null ? result : defs[0], type, getPayload());
  }

  @Override
  public void visit(IVisitor v) {
    v.visitUnspecifiedExpr(this);
  }

  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef(int i) {
    assert i == 0;
    return result;
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int hashCode() {
    return getPayload().hashCode() ^ super.hashCode();
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return result
        + " := (uninterpreted "
        + getPayload().toString().replace(System.lineSeparator(), "")
        + " : "
        + type.getName()
        + ")";
  }
}
