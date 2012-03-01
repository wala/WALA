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

import com.ibm.wala.ssa.SSAAbstractUnaryInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;

public class JavaScriptTypeOfInstruction extends SSAAbstractUnaryInstruction {

  public JavaScriptTypeOfInstruction(int lval, int object) {
    super(lval, object);
  }

  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((JSInstructionFactory)insts).TypeOfInstruction((defs != null ? defs[0] : getDef(0)), (uses != null ? uses[0] : getUse(0)));
  }

  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, getDef(0)) + " = typeof(" + getValueString(symbolTable, getUse(0)) + ")";
  }

  public void visit(IVisitor v) {
    ((JSInstructionVisitor) v).visitTypeOf(this);
  }

  public Collection<TypeReference> getExceptionTypes() {
    return Util.noExceptions();
  }

}
