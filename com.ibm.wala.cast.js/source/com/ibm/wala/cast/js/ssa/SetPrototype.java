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

public class SetPrototype extends SSAInstruction {
  private final int object;
  private final int prototype;

  public SetPrototype(int iindex, int object, int prototype) {
    super(iindex);
    this.object = object;
    this.prototype = prototype;
  }

  @Override
  public int getNumberOfUses() {
    return 2;
  }

  @Override
  public int getUse(int j) throws UnsupportedOperationException {
    assert j >= 0 && j <= 1;
    return (j == 0) ? object : prototype;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((JSInstructionFactory) insts)
        .SetPrototype(
            iIndex(), (uses != null ? uses[0] : object), (uses != null ? uses[1] : prototype));
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "set_prototype("
        + getValueString(symbolTable, object)
        + ", "
        + getValueString(symbolTable, prototype)
        + ')';
  }

  @Override
  public void visit(IVisitor v) {
    ((JSInstructionVisitor) v).visitSetPrototype(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + object;
    result = prime * result + prototype;
    return result;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
