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

import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.Exceptions;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 *
 */
public class SSAGetInstruction extends SSAFieldAccessInstruction {
  private final int result;
  public SSAGetInstruction(int result, int ref, FieldReference field) {
    super(field,ref);
    this.result = result;
  }
  public SSAGetInstruction(int result, FieldReference field) {
    super(field,-1);
    this.result = result;
  }
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    if (isStatic())
      return
        new SSAGetInstruction(
	  defs==null? result: defs[0],
	  getDeclaredField());
    else
      return
        new SSAGetInstruction(
	  defs==null? result: defs[0],
	  uses==null? getRef(): uses[0],
	  getDeclaredField());
  }

   public String toString(SymbolTable symbolTable, ValueDecorator d) {
    if (isStatic()) {
      return getValueString(symbolTable, d, result) + " = getstatic " + getDeclaredField();
    } else {
      return getValueString(symbolTable, d, result) + " = getfield " + getDeclaredField() + " " + getValueString(symbolTable, d, getRef());
    }
  }
  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitGet(this);
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
    return (isStatic()) ? 0 : 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j == 0 && getRef() != -1);
    return getRef();
  }

  public int hashCode() {
    return result * 2371 ^ 6521;
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  public boolean isFallThrough() {
    return true;
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  public Collection getExceptionTypes() {
    return Exceptions.getNullPointerException();
  }
}
