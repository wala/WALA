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

/**
 * @author sfink
 * 
 */
public class SSAArrayStoreInstruction extends SSAArrayReferenceInstruction {

  private final int value;

  public SSAArrayStoreInstruction(int arrayref, int index, int value, TypeReference declaredType) {
    super(arrayref, index, declaredType);
    this.value = value;
  }

  @Override
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    if (uses != null && uses.length < 3) {
      throw new IllegalArgumentException("uses.length < 3");
    }
    return new SSAArrayStoreInstruction(uses == null ? getArrayRef() : uses[0], uses == null ? getIndex() : uses[1],
        uses == null ? value : uses[2], getElementType());
  }

  @Override
  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return "arraystore " + getValueString(symbolTable, d, getArrayRef()) + "[" + getValueString(symbolTable, d, getIndex())
        + "] = " + getValueString(symbolTable, d, value);
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
    v.visitArrayStore(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfUses() {
    return 3;
  }

  @Override
  public int getNumberOfDefs() {
    return 0;
  }

  public int getValue() {
    return value;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    if (j == 2)
      return value;
    else
      return super.getUse(j);
  }

  @Override
  public int hashCode() {
    return 6311 * value ^ 2371 * getArrayRef() + getIndex();
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  @Override
  public Collection<TypeReference> getExceptionTypes() {
    if (typeIsPrimitive()) {
      return Exceptions.getArrayAccessExceptions();
    } else {
      return Exceptions.getAaStoreExceptions();
    }
  }

}
