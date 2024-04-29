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

import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;

/**
 * A read of a global variable denoted by a FieldReference
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class AstGlobalRead extends SSAGetInstruction {

  public AstGlobalRead(int iindex, int lhs, FieldReference global) {
    super(iindex, lhs, global);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstInstructionFactory) insts)
        .GlobalRead(iIndex(), (defs == null) ? getDef() : defs[0], getDeclaredField());
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, getDef()) + " = global:" + getGlobalName();
  }

  @Override
  public void visit(IVisitor v) {
    if (v instanceof AstInstructionVisitor) ((AstInstructionVisitor) v).visitAstGlobalRead(this);
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
