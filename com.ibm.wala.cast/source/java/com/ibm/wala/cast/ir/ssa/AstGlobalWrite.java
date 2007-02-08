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
package com.ibm.wala.cast.ir.ssa;

import java.util.Collection;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.ValueDecorator;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;

public class AstGlobalWrite extends SSAPutInstruction {

  public AstGlobalWrite(FieldReference global, int rhs) {
    super(rhs, global);
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new AstGlobalWrite(getDeclaredField(), (uses==null)? getVal(): uses[0]);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return "global:" + getGlobalName() + " = " + getValueString(symbolTable, d, getVal());
  }

  public void visit(IVisitor v) {
    if (v instanceof AstInstructionVisitor) 
      ((AstInstructionVisitor)v).visitAstGlobalWrite(this);
  }

  public boolean isFallThrough() {
    return true;
  }

  public Collection<TypeReference> getExceptionTypes() {
    return null;
  }

  public String getGlobalName() {
    return getDeclaredField().getName().toString();
  }
}
