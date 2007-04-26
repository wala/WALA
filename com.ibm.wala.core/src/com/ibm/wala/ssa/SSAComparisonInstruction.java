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

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 * 
 */
public class SSAComparisonInstruction extends SSAInstruction {
  private final int result;

  private final int val1;

  private final int val2;

  private final short opcode;

  SSAComparisonInstruction(short opcode, int result, int val1, int val2) {
    super();
    this.opcode = opcode;
    this.result = result;
    this.val1 = val1;
    this.val2 = val2;
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) throws IllegalArgumentException {
    // TODO: Julian ... is this correct?
    if (uses!= null && uses.length != 2) {
      throw new IllegalArgumentException("expected 2 uses, got " + uses.length);
    }
    return new SSAComparisonInstruction(opcode, defs == null || defs.length == 0 ? result : defs[0], uses == null ? val1 : uses[0],
        uses == null ? val2 : uses[1]);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, result) + " = compare " + getValueString(symbolTable, d, val1) + ","
        + getValueString(symbolTable, d, val2) + " opcode=" + opcode;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) throws NullPointerException {
    v.visitComparison(this);
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

  /**
   * @return Returns the opcode.
   */
  public short getOpcode() {
    return opcode;
  }
}
