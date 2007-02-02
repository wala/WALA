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

import java.util.*;

public class EachElementGetInstruction extends SSAAbstractUnaryInstruction {
    
  public EachElementGetInstruction(int lValue, int objectRef) {
    super(lValue, objectRef);
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new EachElementGetInstruction(
      (defs==null)? getDef(0): defs[0],
      (uses == null)? getUse(0): uses[0]);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, getDef(0)) + " = a property name of " + getValueString(symbolTable, d, getUse(0));
  }

  public void visit(IVisitor v) {
    ((AstInstructionVisitor) v).visitEachElementGet(this);
  }

  public Collection getExceptionTypes() {
    return Collections.EMPTY_SET;
  }
}
