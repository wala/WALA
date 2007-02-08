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
public class SSAReturnInstruction extends SSAInstruction {
  
  /**
   * value number of the result.  By convention result == -1 means
   * returns void.
   */
  private final int result;
  private final boolean isPrimitive;
  public SSAReturnInstruction(int result, boolean isPrimitive) {
    super();
    this.result = result;
    this.isPrimitive = isPrimitive;
  }
  public SSAReturnInstruction() {
    super();
    this.result = -1;
    this.isPrimitive = false;
  }
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    if (result == -1)
      return
	new SSAReturnInstruction();
    else
      return
        new SSAReturnInstruction(
          uses==null? result: uses[0],
	  isPrimitive);
  }

   public String toString(SymbolTable table, ValueDecorator d) {
    if (result == -1) {
      return "return";
    } else {
      return "return " + getValueString(table, d, result);
    }
  }
  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitReturn(this);
  }
  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  public int getNumberOfUses() {
    return (result == -1)? 0: 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j == 0);
    return result;
  }
  /**
   * @return true iff this return instruction returns a primitive value
   */
  public boolean returnsPrimitiveType() {
    return isPrimitive;
  }

  public int getResult() {
    return result;
  }

  public boolean returnsVoid() {
    return result == -1;
  }

  public int hashCode() {
    return result * 8933;
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  public boolean isFallThrough() {
    return false;
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  public Collection<TypeReference> getExceptionTypes() {
    return null;
  }
}
