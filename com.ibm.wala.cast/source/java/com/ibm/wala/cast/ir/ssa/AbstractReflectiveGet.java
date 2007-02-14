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
import com.ibm.wala.ssa.ValueDecorator;

/**
 *  This abstract class represents field (a.k.a property) reads in
 * which the field name is not a constant, but rather a computed
 * value.  This is common in scripting languages, and so this base
 * class is shared across all languages that need such accesses.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public abstract class AbstractReflectiveGet extends ReflectiveMemberAccess {
  private final int result;
  
  public AbstractReflectiveGet(int result, int objectRef, int memberRef) {
    super(objectRef, memberRef);
    this.result = result;
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, result) +
	" = " + 
	super.toString(symbolTable, d);
  }
    
  /**
   * @see com.ibm.wala.ssa.Instruction#getDef()
   */
  public boolean hasDef() {
    return true;
  }

  public int getDef() {
    return result;
  }

  public int getDef(int i) {
    return result;
  }

  /**
   * @see com.ibm.wala.ssa.Instruction#getNumberOfUses()
   */
  public int getNumberOfUses() {
    return 2;
  }

  public int getNumberOfDefs() {
    return 1;
  }

}
