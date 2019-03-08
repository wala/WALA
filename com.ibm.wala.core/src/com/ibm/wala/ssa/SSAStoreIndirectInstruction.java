/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ssa;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * A store from a pointer.
 *
 * <p>*p = v
 */
public class SSAStoreIndirectInstruction extends SSAInstruction {

  private final int addressVal;

  private final int rval;

  private final TypeReference pointeeType;

  /**
   * @param addressVal the value number holding the pointer p deferenced (*p)
   * @param rval the value number which is stored into the pointer location
   */
  public SSAStoreIndirectInstruction(
      int iindex, int addressVal, int rval, TypeReference pointeeType) {
    super(iindex);
    this.addressVal = addressVal;
    this.rval = rval;
    this.pointeeType = pointeeType;
  }

  public TypeReference getPointeeType() {
    return pointeeType;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    Assertions.UNREACHABLE("unimplemented");
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
    return '*'
        + getValueString(symbolTable, addressVal)
        + " = "
        + getValueString(symbolTable, rval);
  }

  @Override
  public void visit(IVisitor v) {
    ((IVisitorWithAddresses) v).visitStoreIndirect(this);
  }
}
