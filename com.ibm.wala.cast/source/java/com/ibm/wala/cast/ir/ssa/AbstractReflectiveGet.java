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
 * This abstract class represents field (a.k.a property) reads in which the field name is not a constant, but rather a
 * computed value. This is common in scripting languages, and so this base class is shared across all languages that
 * need such accesses.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public abstract class AbstractReflectiveGet extends ReflectiveMemberAccess {
  private final int result;

  public AbstractReflectiveGet(int iindex, int result, int objectRef, int memberRef) {
    super(iindex, objectRef, memberRef);
    this.result = result;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result) + " = " + super.toString(symbolTable);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public int getDef(int i) {
    return result;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfUses() {
    return 2;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

}
