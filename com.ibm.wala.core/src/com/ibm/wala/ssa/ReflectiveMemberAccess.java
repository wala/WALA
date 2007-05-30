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

import com.ibm.wala.util.debug.Assertions;

/**
 * @author Julian Dolby
 * 
 * TODO: document me
 */
public abstract class ReflectiveMemberAccess extends SSAInstruction {
  protected final int objectRef;

  protected final int memberRef;

  /**
   * @param objectRef
   * @param memberRef
   */
  protected ReflectiveMemberAccess(int objectRef, int memberRef) {
    super();
    this.objectRef = objectRef;
    this.memberRef = memberRef;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.SSAInstruction#toString(com.ibm.wala.ssa.SymbolTable,
   *      com.ibm.wala.ssa.ValueDecorator)
   */
  @Override
  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return "fieldref " + getValueString(symbolTable, d, objectRef) + "." + getValueString(symbolTable, d, memberRef);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j <= 1);
    return (j == 0) ? objectRef : memberRef;
  }

  public int getObjectRef() {
    return objectRef;
  }

  public int getMemberRef() {
    return memberRef;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return 6311 * memberRef ^ 2371 * objectRef;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.SSAInstruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

}