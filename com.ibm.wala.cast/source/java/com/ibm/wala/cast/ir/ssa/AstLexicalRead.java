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


import com.ibm.wala.ssa.*;
import com.ibm.wala.util.debug.Assertions;

public class AstLexicalRead extends AstLexicalAccess {

  public AstLexicalRead(Access[] accesses) {
    super(accesses);
  }

  public AstLexicalRead(Access access) {
    this(new Access[]{ access });
  }

  public AstLexicalRead(int lhs, String definer, String globalName) {
    this(new Access(globalName, definer, lhs));
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    if (defs==null) {
      return new AstLexicalRead( getAccesses() );
    } else {
      Access[] accesses = new Access[ getAccessCount() ];
      for(int i = 0; i < accesses.length; i++) {
	Access oldAccess = getAccess(i);
	accesses[i] = new Access(oldAccess.variableName, oldAccess.variableDefiner, defs[i]);
      }

      return new AstLexicalRead(accesses);
    }
  }

  public int getNumberOfDefs() { return getAccessCount(); }

  public int getDef(int i) { return getAccess(i).valueNumber; }

  public int getNumberOfUses() { return 0; }

  public int getUse(int i) { throw new UnsupportedOperationException(); }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    StringBuffer sb = new StringBuffer();
    for(int i = 0; i < getAccessCount(); i++) {
      Access A = getAccess(i);
      if (i != 0) sb.append(", ");
      sb.append(getValueString(symbolTable, d, A.valueNumber));
      sb.append(" = lexical:");
      sb.append(A.variableName);
      sb.append("@");
      sb.append(A.variableDefiner);
    }

    return sb.toString();
  }
    
  public void visit(IVisitor v) {
    Assertions._assert(v instanceof AstInstructionVisitor);
    ((AstInstructionVisitor)v).visitAstLexicalRead(this);
  }
}
