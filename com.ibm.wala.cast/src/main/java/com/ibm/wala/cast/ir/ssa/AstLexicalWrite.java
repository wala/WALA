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
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;

/**
 * A set of lexical writes. This instruction represents writes of a set of variables that are
 * defined by a pair of variable name and defining code body (i.e. a method or function). This
 * instruction has one local value number use for each lexical write, and the call graph builder
 * ensures that these value numbers are kept consistent as lexical uses and definitions are
 * discovered during call graph construction.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class AstLexicalWrite extends AstLexicalAccess {

  public AstLexicalWrite(
      int iindex, String definer, String globalName, TypeReference type, int rhs) {
    this(iindex, new Access(globalName, definer, type, rhs));
  }

  public AstLexicalWrite(int iindex, Access access) {
    this(iindex, new Access[] {access});
  }

  public AstLexicalWrite(int iindex, Access[] accesses) {
    super(iindex, accesses);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    if (uses == null) {
      return new AstLexicalWrite(iIndex(), getAccesses());
    } else {
      Access[] accesses = new Access[getAccessCount()];
      for (int i = 0; i < accesses.length; i++) {
        Access oldAccess = getAccess(i);
        accesses[i] =
            new Access(oldAccess.variableName, oldAccess.variableDefiner, oldAccess.type, uses[i]);
      }

      return ((AstInstructionFactory) insts).LexicalWrite(iIndex(), accesses);
    }
  }

  @Override
  public int getNumberOfUses() {
    return getAccessCount();
  }

  @Override
  public int getUse(int i) {
    return getAccess(i).valueNumber;
  }

  @Override
  public int getNumberOfDefs() {
    return 0;
  }

  @Override
  public int getDef(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < getAccessCount(); i++) {
      Access A = getAccess(i);
      if (i != 0) sb.append(", ");
      sb.append("lexical:");
      sb.append(A.variableName);
      sb.append('@');
      sb.append(A.variableDefiner);
      sb.append(" = ");
      sb.append(getValueString(symbolTable, A.valueNumber));
    }

    return sb.toString();
  }

  @Override
  public void visit(IVisitor v) {
    assert v instanceof AstInstructionVisitor;
    ((AstInstructionVisitor) v).visitAstLexicalWrite(this);
  }
}
