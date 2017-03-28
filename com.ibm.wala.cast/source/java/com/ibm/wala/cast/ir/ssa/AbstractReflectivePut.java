/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.ReflectiveMemberAccess;
import com.ibm.wala.ssa.SymbolTable;

/**
 * This abstract class represents field (a.k.a property) writes in which the field name is not a constant, but rather a
 * computed value. This is common in scripting languages, and so this base class is shared across all languages that
 * need such accesses.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public abstract class AbstractReflectivePut extends ReflectiveMemberAccess {
  private final int value;

  public AbstractReflectivePut(int iindex, int objectRef, int memberRef, int value) {
    super(iindex, objectRef, memberRef);
    this.value = value;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return super.toString(symbolTable) + " = " + getValueString(symbolTable, value);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  @Override
  public int getDef() {
    return -1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfUses() {
    return 3;
  }

  public int getValue() {
    return getUse(2);
  }

  @Override
  public int getUse(int index) {
    if (index == 2)
      return value;
    else
      return super.getUse(index);
  }

}
