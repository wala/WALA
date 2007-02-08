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

import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Exceptions;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 * 
 */
public class SSABinaryOpInstruction extends SSAInstruction {

  private final int result;

  private final int val1;

  private final int val2;

  private final BinaryOpInstruction.IOperator operator;

  SSABinaryOpInstruction(BinaryOpInstruction.IOperator operator, int result, int val1, int val2) {
    super();
    this.result = result;
    this.val1 = val1;
    this.val2 = val2;
    this.operator = operator;
    Assertions._assert(val1 != -1 && val2 != -1);
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new SSABinaryOpInstruction(operator, defs == null ? result : defs[0], uses == null ? val1 : uses[0], uses == null ? val2
        : uses[1]);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, result) + 
	" = binaryop(" + operator + ") " + 
	getValueString(symbolTable, d, val1) + " , " +
	getValueString(symbolTable, d, val2);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitBinaryOp(this);
  }

  /**
   * UGH! This must be the Shrike OPCODE, not the Shrike OPERATOR code!!!!!
   * 
   * @artifact 38486
   * @return instruction opcode
   */
  public BinaryOpInstruction.IOperator getOperator() {
    return operator;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  public boolean hasDef() {
    return true;
  }

  public int getDef() {
    return result;
  }

  public int getDef(int i) {
    Assertions._assert(i == 0);
    return result;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  public int getNumberOfDefs() {
    return 1;
  }

  public int getNumberOfUses() {
    return 2;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j <= 1);
    return (j == 0) ? val1 : val2;
  }

  public int hashCode() {
    return 6311 * result ^ 2371 * val1 + val2;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.Instruction#isPEI()
   */
  public boolean isPEI() {
    return operator == BinaryOpInstruction.Operator.DIV || operator == BinaryOpInstruction.Operator.REM;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  public boolean isFallThrough() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  public Collection<TypeReference> getExceptionTypes() {
    if (isPEI()) {
      return Exceptions.getArithmeticException();
    } else {
      return null;
    }
  }

}
