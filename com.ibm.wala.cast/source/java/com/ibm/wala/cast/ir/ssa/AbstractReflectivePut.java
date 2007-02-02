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


import com.ibm.wala.ssa.*;
import com.ibm.wala.util.debug.Assertions;

public abstract class AbstractReflectivePut extends ReflectiveMemberAccess {
  private final int value;
  
  public AbstractReflectivePut(int objectRef, int memberRef, int value) {
    super(objectRef, memberRef);
    this.value = value;
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return super.toString(symbolTable, d) +
	" = " + 
	getValueString(symbolTable, d, value);
  }

  /**
   * @see com.ibm.wala.ssa.Instruction#getDef()
   */
  public int getDef() {
    return -1;
  }

  /**
   * @see com.ibm.wala.ssa.Instruction#getNumberOfUses()
   */
  public int getNumberOfUses() {
    return 3;
  }

  public int getValue() {
    return getUse(2);
  }

  public int getUse(int index) {
    if (index == 2)
      return value;
    else
      return super.getUse(index);
  }

}
