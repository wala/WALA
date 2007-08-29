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
import com.ibm.wala.types.TypeReference;

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

  @Override
  public SSAInstruction copyForSSA(int[] defs, int[] uses) throws IllegalArgumentException {
    if (uses != null && uses.length == 0) {
      throw new IllegalArgumentException("(uses != null) and (uses.length == 0)");
    }
    return new SSAUnaryOpInstruction(operator, defs == null || defs.length == 0 ? result : defs[0], uses == null ? val : uses[0]);
  }

  @Override
  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, result) + " = " + operator + " " + getValueString(symbolTable, d, val);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitUnaryOp(this);
  }

  public UnaryOpInstruction.IOperator getOpcode() {
    return operator;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return null;
  }
}
