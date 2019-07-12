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

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import java.util.Collection;
import java.util.Collections;

public class JavaScriptInstanceOf extends SSAInstruction {
  private final int objVal;
  private final int typeVal;
  private final int result;

  public JavaScriptInstanceOf(int iindex, int result, int objVal, int typeVal) {
    super(iindex);
    this.objVal = objVal;
    this.typeVal = typeVal;
    this.result = result;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((JSInstructionFactory) insts)
        .InstanceOf(
            iIndex(),
            defs == null ? result : defs[0],
            uses == null ? objVal : uses[0],
            uses == null ? typeVal : uses[1]);
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Collections.singleton(JavaScriptTypes.TypeError);
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public int hashCode() {
    return objVal * 31771 + typeVal * 23 + result;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result)
        + " = "
        + getValueString(symbolTable, objVal)
        + " is instance of "
        + getValueString(symbolTable, typeVal);
  }

  @Override
  public void visit(IVisitor v) {
    ((JSInstructionVisitor) v).visitJavaScriptInstanceOf(this);
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int getDef(int i) {
    assert i == 0;
    return result;
  }

  @Override
  public int getNumberOfUses() {
    return 2;
  }

  @Override
  public int getUse(int i) {
    switch (i) {
      case 0:
        return objVal;
      case 1:
        return typeVal;
      default:
        Assertions.UNREACHABLE();
        return -1;
    }
  }
}
