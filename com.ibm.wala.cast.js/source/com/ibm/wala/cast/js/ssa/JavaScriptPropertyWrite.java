/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.ssa;

import java.util.Collection;

import com.ibm.wala.cast.ir.ssa.AbstractReflectivePut;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;

public class JavaScriptPropertyWrite extends AbstractReflectivePut {

  public JavaScriptPropertyWrite(int iindex, int objectRef, int memberRef, int value) {
    super(iindex, objectRef, memberRef, value);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((JSInstructionFactory)insts).PropertyWrite(iindex, uses == null ? getObjectRef() : uses[0], uses == null ? getMemberRef() : uses[1],
        uses == null ? getValue() : uses[2]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return super.toString(symbolTable) + " = " + getValueString(symbolTable, getValue());
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  @Override
  public void visit(IVisitor v) {
    assert v instanceof JSInstructionVisitor;
    ((JSInstructionVisitor) v).visitJavaScriptPropertyWrite(this);
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#isPEI()
   */
  @Override
  public boolean isPEI() {
    return true;
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#getExceptionTypes()
   */
  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Util.typeErrorExceptions();
  }

}
