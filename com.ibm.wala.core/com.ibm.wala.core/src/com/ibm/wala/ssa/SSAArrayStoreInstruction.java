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

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new SSAArrayStoreInstruction(
      uses == null ? getArrayRef() : uses[0],
      uses == null ? getIndex() : uses[1],
      uses == null ? value : uses[2],
      getDeclaredType());
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return "arraystore " + getValueString(symbolTable, d, getArrayRef()) + "[" + getValueString(symbolTable, d, getIndex()) + "] = " + getValueString(symbolTable, d, value);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitArrayStore(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  public int getNumberOfUses() {
    return 3;
  }

  public int getNumberOfDefs() {
    return 0;
  }

  public int getValue() {
    return value;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  public int getUse(int j) {
    if (j == 2)
      return value;
    else
      return super.getUse(j);
  }

  public int hashCode() {
    return 6311 * value ^ 2371 * getArrayRef() + getIndex();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  public Collection getExceptionTypes() {
    if (typeIsPrimitive()) {
      return Exceptions.getArrayAccessExceptions();
    } else {
      return Exceptions.getAaStoreExceptions();
    }
  }

}
