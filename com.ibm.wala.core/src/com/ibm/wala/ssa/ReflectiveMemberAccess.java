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


/**
 * TODO: document me.
 */
public abstract class ReflectiveMemberAccess extends SSAInstruction {
  protected final int objectRef;

  protected final int memberRef;

  protected ReflectiveMemberAccess(int iindex, int objectRef, int memberRef) {
    super(iindex);
    this.objectRef = objectRef;
    this.memberRef = memberRef;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "fieldref " + getValueString(symbolTable, objectRef) + "." + getValueString(symbolTable, memberRef);
  }

  /*
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    assert j <= 1;
    return (j == 0) ? objectRef : memberRef;
  }

  public int getObjectRef() {
    return objectRef;
  }

  public int getMemberRef() {
    return memberRef;
  }

  @Override
  public int hashCode() {
    return 6311 * memberRef ^ 2371 * objectRef;
  }

  /*
   * @see com.ibm.wala.ssa.SSAInstruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

}
