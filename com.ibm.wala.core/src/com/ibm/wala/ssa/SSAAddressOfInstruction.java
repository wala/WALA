/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import com.ibm.wala.types.FieldReference;

public class SSAAddressOfInstruction extends SSAInstruction {

  private final int lval;
  private final int addressVal;
  private final int indexVal;
  private final FieldReference field;
  
  public SSAAddressOfInstruction(int lval, int local) {
    this.lval = lval;
    this.addressVal = local;
    this.indexVal = -1;
    this.field = null;
  }

  public SSAAddressOfInstruction(int lval, int local, int indexVal) {
    this.lval = lval;
    this.addressVal = local;
    this.indexVal = indexVal;
    this.field = null;
  }

  public SSAAddressOfInstruction(int lval, int local, FieldReference field) {
    this.lval = lval;
    this.addressVal = local;
    this.indexVal = -1;
    this.field = field;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int hashCode() {
     return lval * 99701 * addressVal;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, lval) + " = &" + getValueString(symbolTable, addressVal) +
      ( (indexVal != -1)? "[" + getValueString(symbolTable, indexVal) + "]":
        (field != null)? "." + field.getName().toString(): "");
  }

  @Override
  public void visit(IVisitor v) {
    v.visitAddressOf(this);
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }
  
  @Override
  public int getDef(int i) {
    assert i == 0;
    return lval;
  }

  @Override
  public int getDef() {
    return lval;
  }

  @Override
  public int getNumberOfUses() {
    return (indexVal == -1)? 1: 2;
  }
  
  @Override
  public int getUse(int i) {
    assert i == 0 || (i == 1 && indexVal != -1);
    if (i == 0) { 
      return addressVal;
    } else {
      return indexVal;
    }
  }
}
