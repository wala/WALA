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

import com.ibm.wala.analysis.stackMachine.AbstractIntStackMachine;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * Note: if getUse(i) returns {@link AbstractIntStackMachine}.TOP (that is, -1), then that use represents
 * an edge in the CFG which is infeasible in verifiable bytecode.
 * 
 * @author sfink
 * 
 */
public class SSAPhiInstruction extends SSAInstruction {
  private final int result;

  private int[] params;

  public SSAPhiInstruction(int result, int[] params) throws IllegalArgumentException {
    super();
    if (params == null) {
      throw new IllegalArgumentException("params is null");
    }
    this.result = result;
    this.params = params;
    if (params.length == 0) {
      throw new IllegalArgumentException("can't have phi with no params");
    }
    for (int p : params) {
      if (p == 0) {
        throw new IllegalArgumentException("zero is an invalid value number for a parameter to phi");
      }
    }
  }

  @Override
  public SSAInstruction copyForSSA(int[] defs, int[] uses) throws IllegalArgumentException {
    if (defs != null && defs.length == 0) {
      throw new IllegalArgumentException();
    }
    return new SSAPhiInstruction(defs == null ? result : defs[0], uses == null ? params : uses);
  }

  @Override
  public String toString(SymbolTable symbolTable) {

    StringBuffer s = new StringBuffer();

    s.append(getValueString(symbolTable, result)).append(" = phi ");
    s.append(" ").append(getValueString(symbolTable, params[0]));
    for (int i = 1; i < params.length; i++) {
      s.append(",").append(getValueString(symbolTable, params[i]));
    }
    return s.toString();
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * @throws IllegalArgumentException
   *             if v is null
   */
  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitPhi(this);
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
  public int getNumberOfUses() {
    return params.length;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) throws IllegalArgumentException {
    if (j >= params.length) {
      throw new IllegalArgumentException("Bad use " + j);
    }
    return params[j];
  }

  /**
   * @param i
   */
  public void setValues(int[] i) {
    this.params = i;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getValueString(SymbolTable,
   *      ValueDecorator, int)
   */
  @Override
  protected String getValueString(SymbolTable symbolTable,int valueNumber) {
    if (valueNumber == AbstractIntStackMachine.TOP) {
      return "TOP";
    } else {
      return super.getValueString(symbolTable, valueNumber);
    }
  }

  @Override
  public int hashCode() {
    return 7823 * result;
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
}
