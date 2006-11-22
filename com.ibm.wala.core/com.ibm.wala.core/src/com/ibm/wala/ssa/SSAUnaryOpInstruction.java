/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.Collection;

import com.ibm.wala.shrikeBT.UnaryOpInstruction;

/**
 * @author sfink
 *
 */
public class SSAUnaryOpInstruction extends SSAAbstractUnaryInstruction {

  private final UnaryOpInstruction.IOperator operator;

  protected SSAUnaryOpInstruction(UnaryOpInstruction.IOperator operator, int result, int val) {
    super(result, val);
    this.operator = operator;
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return
      new SSAUnaryOpInstruction(
        operator,
	defs==null? result: defs[0],
	uses==null? val: uses[0]);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, result) + " = " + operator + " " + getValueString(symbolTable, d, val);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitUnaryOp(this);
  }

  public UnaryOpInstruction.IOperator getOpcode() {
  	return operator;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  public Collection getExceptionTypes() {
    return null;
  }
}
