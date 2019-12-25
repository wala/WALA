/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;

public class JavaScriptWithRegion extends SSAInstruction {
  private final int expr;
  private final boolean isEnter;

  public JavaScriptWithRegion(int iindex, int expr, boolean isEnter) {
    super(iindex);
    this.expr = expr;
    this.isEnter = isEnter;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((JSInstructionFactory) insts)
        .WithRegion(iIndex(), uses == null ? expr : uses[0], isEnter);
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return null;
  }

  @Override
  public int hashCode() {
    return 353456 * expr * (isEnter ? 1 : -1);
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return (isEnter ? "enter" : "exit") + " of with " + getValueString(symbolTable, expr);
  }

  @Override
  public void visit(IVisitor v) {
    ((JSInstructionVisitor) v).visitWithRegion(this);
  }

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getUse(int i) {
    assert i == 0;
    return expr;
  }
}
