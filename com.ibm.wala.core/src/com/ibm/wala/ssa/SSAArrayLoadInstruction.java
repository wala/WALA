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

import com.ibm.wala.types.*;
import com.ibm.wala.util.Exceptions;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 *  
 */
public class SSAArrayLoadInstruction extends SSAArrayReferenceInstruction {
  private final int result;

  SSAArrayLoadInstruction(int result, int arrayref, int index, TypeReference declaredType) {
    super(arrayref, index, declaredType);
    this.result = result;
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new SSAArrayLoadInstruction(
      defs == null ? result : defs[0],
      uses == null ? getArrayRef() : uses[0],
      uses == null ? getIndex() : uses[1], 
      getDeclaredType());
  }
    
  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, result) + " = arrayload " + getValueString(symbolTable, d, getArrayRef()) + "[" + getValueString(symbolTable, d, getIndex()) + "]";
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitArrayLoad(this);
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

  public int getNumberOfDefs() {
    return 1;
  }

  public int hashCode() {
    return 6311 * result ^ 2371 * getArrayRef() + getIndex();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  public Collection getExceptionTypes() {
    return Exceptions.getArrayAccessExceptions();
  }

}
