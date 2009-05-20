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

public class SSAStoreIndirectInstruction extends SSAInstruction {

  private final int addressVal;
  private final int rval;
  
  public SSAStoreIndirectInstruction(int addressVal, int rval) {
    this.addressVal = addressVal;
    this.rval = rval;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int hashCode() {
    return addressVal * 353456 * rval;
  }

  @Override
  public boolean isFallThrough() {
     return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "*" + getValueString(symbolTable, addressVal) + " = " + getValueString(symbolTable, rval);
  }

  @Override
  public void visit(IVisitor v) {
    // TODO Auto-generated method stub

  }

}
