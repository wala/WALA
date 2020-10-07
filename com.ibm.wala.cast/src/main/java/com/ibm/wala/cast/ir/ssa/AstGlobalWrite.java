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
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;

/**
 * A write of a global variable denoted by a FieldReference
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class AstGlobalWrite extends SSAPutInstruction {

  public AstGlobalWrite(int iindex, FieldReference global, int rhs) {
    super(iindex, rhs, global);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstInstructionFactory) insts)
        .GlobalWrite(iIndex(), getDeclaredField(), (uses == null) ? getVal() : uses[0]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "global:" + getGlobalName() + " = " + getValueString(symbolTable, getVal());
  }

  @Override
  public void visit(IVisitor v) {
    if (v instanceof AstInstructionVisitor) ((AstInstructionVisitor) v).visitAstGlobalWrite(this);
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return null;
  }

  public String getGlobalName() {
    return getDeclaredField().getName().toString();
  }
}
