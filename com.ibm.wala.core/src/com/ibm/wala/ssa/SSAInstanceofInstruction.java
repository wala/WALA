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
public class SSAInstanceofInstruction extends SSAInstruction {
  private final int result;
  private final int ref;
  private final TypeReference checkedType;
  SSAInstanceofInstruction(int result, int ref, TypeReference checkedType) {
    super();
    this.result = result;
    this.ref = ref;
    this.checkedType = checkedType;
  }
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return
      new SSAInstanceofInstruction(
        defs==null? result: defs[0],
	uses==null? ref: uses[0],
	checkedType);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, result) + " = instanceof " + getValueString(symbolTable, d, ref) + " " + checkedType;
  }
  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitInstanceof(this);
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

  public TypeReference getCheckedType() {
    return checkedType;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  public int getNumberOfDefs() {
    return 1;
  }

  public int getNumberOfUses() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j == 0);
    return ref;
  }

  public int hashCode() {
    return ref * 677 ^ result * 3803;
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
  public Collection<TypeReference> getExceptionTypes() {
    return null;
  }
  /**
   * @return Returns the ref.
   */
  public int getRef() {
    return ref;
  }
}
