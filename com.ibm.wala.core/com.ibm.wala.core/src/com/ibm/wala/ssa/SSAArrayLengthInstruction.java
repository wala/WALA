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

import com.ibm.wala.util.Exceptions;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 *  
 */
public class SSAArrayLengthInstruction extends SSAInstruction {
  private final int result;

  private final int arrayref;

  public SSAArrayLengthInstruction(int result, int arrayref) {
    super();
    this.result = result;
    this.arrayref = arrayref;
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new SSAArrayLengthInstruction(defs == null ? result : defs[0], uses == null ? arrayref : uses[0]);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, result) + " = arraylength " + getValueString(symbolTable, d, arrayref);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitArrayLength(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  public int getDef() {
    return result;
  }

  public boolean hasDef() {
    return true;
  }

  public int getDef(int i) {
    Assertions._assert(i == 0);
    return result;
  }

  public int getNumberOfDefs() {
    return 1;
  }

  public int getArrayRef() {
    return arrayref;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  public int getNumberOfUses() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j == 0);
    return arrayref;
  }

  public int hashCode() {
    return arrayref * 7573 + result * 563;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.Instruction#isPEI()
   */
  public boolean isPEI() {
    return true;
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
  public Collection getExceptionTypes() {
    return Exceptions.getNullPointerException();
  }

}
