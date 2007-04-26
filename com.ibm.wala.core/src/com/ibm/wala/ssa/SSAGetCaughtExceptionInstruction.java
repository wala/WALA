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
public class SSAGetCaughtExceptionInstruction extends SSAInstruction {
  private final int exceptionValueNumber;

  private final int bbNumber;

  SSAGetCaughtExceptionInstruction(int bbNumber, int exceptionValueNumber) {
    super();
    this.exceptionValueNumber = exceptionValueNumber;
    this.bbNumber = bbNumber;
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    // TODO: check that this is the intended behavior. Julian?
    return new SSAGetCaughtExceptionInstruction(bbNumber, defs == null || defs.length == 0 ? exceptionValueNumber : defs[0]);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {

    StringBuffer s = new StringBuffer();
    s.append(getValueString(symbolTable, d, exceptionValueNumber)).append(" = getCaughtException ");
    return s.toString();
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitGetCaughtException(this);
  }

  /**
   * Returns the result.
   * 
   * @return int
   */
  public int getException() {
    return exceptionValueNumber;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  public boolean hasDef() {
    return true;
  }

  public int getDef() {
    return exceptionValueNumber;
  }

  public int getDef(int i) {
    Assertions._assert(i == 0);
    return exceptionValueNumber;
  }

  public int getNumberOfDefs() {
    return 1;
  }

  /**
   * Returns the bb.
   * 
   * @return BasicBlock
   */
  public int getBasicBlockNumber() {
    return bbNumber;
  }

  public int hashCode() {
    return 2243 * exceptionValueNumber;
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
