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
import com.ibm.wala.util.Exceptions;
import com.ibm.wala.util.debug.Assertions;

/**
 * SSA instruction representing v_x := arraylength v_y
 * 
 * @author sfink
 */
public class SSAArrayLengthInstruction extends SSAInstruction {
  private final int result;

  private final int arrayref;

  public SSAArrayLengthInstruction(int result, int arrayref) {
    super();
    this.result = result;
    this.arrayref = arrayref;
  }

  @Override
  public SSAInstruction copyForSSA(int[] defs, int[] uses) throws IllegalArgumentException {
    if (defs != null && defs.length != 1) {
      throw new IllegalArgumentException();
    }
    if (uses != null && uses.length != 1) {
      throw new IllegalArgumentException();
    }
    return new SSAArrayLengthInstruction(defs == null ? result : defs[0], uses == null ? arrayref : uses[0]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result) + " = arraylength " + getValueString(symbolTable, arrayref);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitArrayLength(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  @Override
  public int getDef() {
    return result;
  }

  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef(int i) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(i == 0);
    }
    return result;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  public int getArrayRef() {
    return arrayref;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfUses() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j == 0);
    return arrayref;
  }

  @Override
  public int hashCode() {
    return arrayref * 7573 + result * 563;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isPEI()
   */
  @Override
  public boolean isPEI() {
    return true;
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
    return Exceptions.getNullPointerException();
  }

}
