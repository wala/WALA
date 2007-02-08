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

import com.ibm.wala.analysis.stackMachine.AbstractIntStackMachine;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 *  
 */
public class SSAPhiInstruction extends SSAInstruction {
  private final int result;

  private int[] params;

  public SSAPhiInstruction(int result, int[] params) {
    super();
    this.result = result;
    this.params = params;
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) throws IllegalArgumentException {
    if (defs != null && defs.length == 0) {
      throw new IllegalArgumentException();
    }
    return new SSAPhiInstruction(defs == null ? result : defs[0], uses == null ? params : uses);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {

    StringBuffer s = new StringBuffer();

    s.append(getValueString(symbolTable, d, result)).append(" = phi ");
    s.append(" ").append(getValueString(symbolTable, d, params[0]));
    for (int i = 1; i < params.length; i++) {
      s.append(",").append(getValueString(symbolTable, d, params[i]));
    }
    return s.toString();
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitPhi(this);
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
  public int getNumberOfUses() {
    return params.length;
  }

  public int getNumberOfDefs() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  public int getUse(int j) throws IllegalArgumentException {
    if (j >= params.length) {
      throw new IllegalArgumentException("Bad use " + j);
    }
    return params[j];
  }

  /**
   * Method setValues.
   * 
   * @param i
   */
  public void setValues(int[] i) {
    this.params = i;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getValueString(SymbolTable,
   *      ValueDecorator, int)
   */
  protected String getValueString(SymbolTable symbolTable, ValueDecorator d, int valueNumber) {

    if (valueNumber == AbstractIntStackMachine.TOP) {
      return "TOP";
    } else {
      return super.getValueString(symbolTable, d, valueNumber);
    }
  }

  public int hashCode() {
    return 7823 * result;
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
