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

import com.ibm.wala.ssa.*;
import com.ibm.wala.types.*;

public class AstGlobalRead extends SSAGetInstruction {

  public AstGlobalRead(int lhs, FieldReference global) {
    super(lhs, global);
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new AstGlobalRead((defs==null)? getDef(): defs[0], getDeclaredField());
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, getDef()) + " = global:" + getGlobalName();
  }

  public void visit(IVisitor v) {
    if (v instanceof AstInstructionVisitor) 
      ((AstInstructionVisitor)v).visitAstGlobalRead(this);
  }

  public boolean isFallThrough() {
    return true;
  }

  public Collection getExceptionTypes() {
    return null;
  }

  public String getGlobalName() {
    return getDeclaredField().getName().toString();
  }
}
