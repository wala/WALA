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
import java.util.Collections;

import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * SSA Instruction for comparisons between floats, longs and doubles
 * 
 * @author sfink
 */
public class SSAComparisonInstruction extends SSAInstruction {
  private final int result;

  private final int val1;

  private final int val2;

  private final IComparisonInstruction.Operator operator;

  /**
   * @param opcode
   *            opcode from list in {@link Constants}
   */
  SSAComparisonInstruction(IComparisonInstruction.Operator operator, int result, int val1, int val2) {
    super();
    this.operator = operator;
    this.result = result;
    this.val1 = val1;
    this.val2 = val2;
  }

  @Override
  public SSAInstruction copyForSSA(int[] defs, int[] uses) throws IllegalArgumentException {
    // TODO: Julian ... is this correct?
    if (uses != null && uses.length != 2) {
      throw new IllegalArgumentException("expected 2 uses, got " + uses.length);
    }
    return new SSAComparisonInstruction(operator, defs == null || defs.length == 0 ? result : defs[0], uses == null ? val1 : uses[0],
        uses == null ? val2 : uses[1]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result) + " = compare " + getValueString(symbolTable, val1) + ","
        + getValueString(symbolTable, val2) + " opcode=" + operator;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitComparison(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public int getDef(int i) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(i == 0);
    }
    return result;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int getNumberOfUses() {
    return 2;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j <= 1);
    return (j == 0) ? val1 : val2;
  }

  @Override
  public int hashCode() {
    return 6311 * result ^ 2371 * val1 + val2;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Collections.emptySet();
  }

  /**
   * @return Returns the opcode.
   */
  public IComparisonInstruction.Operator getOperator() {
    return operator;
  }
}
