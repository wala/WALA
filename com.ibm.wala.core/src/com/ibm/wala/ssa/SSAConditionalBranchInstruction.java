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

import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 * 
 */
public class SSAConditionalBranchInstruction extends SSAInstruction {
  private final ConditionalBranchInstruction.IOperator operator;

  private final int val1;

  private final int val2;

  private final TypeReference type;

  SSAConditionalBranchInstruction(ConditionalBranchInstruction.IOperator operator, TypeReference type, int val1, int val2) throws IllegalArgumentException {
    super();
    this.operator = operator;
    this.val1 = val1;
    this.val2 = val2;
    this.type = type;
    if (val1 <= 0) {
      throw new IllegalArgumentException("Invalid val1: " + val1);
    }
    if (val2 <= 0) {
      throw new IllegalArgumentException("Invalid val2: " + val2);
    }
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new SSAConditionalBranchInstruction(operator, type, uses == null ? val1 : uses[0], uses == null ? val2 : uses[1]);
  }

  public ConditionalBranchInstruction.IOperator getOperator() {
    return operator;
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return "conditional branch(" + operator + ") " + getValueString(symbolTable, d, val1) + ","
        + getValueString(symbolTable, d, val2);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitConditionalBranch(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
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

  public boolean isObjectComparison() {
    return type == TypeReference.JavaLangObject;
  }

  public boolean isIntegerComparison() {
    return type == TypeReference.Int;
  }

  public int hashCode() {
    return 7151 * val1 + val2;
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
    return null;
  }
}
