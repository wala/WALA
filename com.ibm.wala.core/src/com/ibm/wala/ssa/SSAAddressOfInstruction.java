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

import com.ibm.wala.ssa.SSAIndirectionData.Name;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * An {@link SSAAddressOfInstruction} represents storing the address of some "source" level entity
 * (@see {@link Name}) into an SSA value number.
 */
public class SSAAddressOfInstruction extends SSAInstruction {

  /**
   * The value number which is def'ed ... this instruction assigns this value to hold an address.
   */
  private final int lval;

  /**
   * The SSA value number that represents the entity whose address is being taken.
   *
   * <p>If we're taking the address of a local variable, this is the value number representing that
   * local variable immediately before this instruction.
   *
   * <p>If we're taking the address of an array element or a field of an object, then this is the
   * base pointer.
   */
  private final int addressVal;

  /**
   * If we're taking the address of an array element, this is the array index. Otherwise, this is
   * -1.
   */
  private final int indexVal;

  /**
   * If we're taking the address of a field, this is the field reference. Otherwise, this is null.
   */
  private final FieldReference field;

  private final TypeReference pointeeType;

  /** Use this constructor when taking the address of a local variable. */
  public SSAAddressOfInstruction(int iindex, int lval, int local, TypeReference pointeeType) {
    super(iindex);
    if (local <= 0) {
      throw new IllegalArgumentException("Invalid local address load of " + local);
    }
    this.lval = lval;
    this.addressVal = local;
    this.indexVal = -1;
    this.field = null;
    this.pointeeType = pointeeType;
  }

  /** Use this constructor when taking the address of an array element. */
  public SSAAddressOfInstruction(
      int iindex, int lval, int basePointer, int indexVal, TypeReference pointeeType) {
    super(iindex);
    this.lval = lval;
    this.addressVal = basePointer;
    this.indexVal = indexVal;
    this.field = null;
    this.pointeeType = pointeeType;
  }

  /** Use this constructor when taking the address of a field in an object. */
  public SSAAddressOfInstruction(
      int iindex, int lval, int basePointer, FieldReference field, TypeReference pointeeType) {
    super(iindex);
    this.lval = lval;
    this.addressVal = basePointer;
    this.indexVal = -1;
    this.field = field;
    this.pointeeType = pointeeType;
  }

  public TypeReference getType() {
    return pointeeType;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    Assertions.UNREACHABLE("not yet implemented.  to be nuked");
    return null;
  }

  @Override
  public int hashCode() {
    return lval * 99701 + addressVal;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, lval)
        + " ("
        + pointeeType.getName()
        + ") "
        + " = &"
        + getValueString(symbolTable, addressVal)
        + ((indexVal != -1)
            ? '[' + getValueString(symbolTable, indexVal) + ']'
            : (field != null) ? '.' + field.getName().toString() : "");
  }

  @Override
  public void visit(IVisitor v) {
    assert (v instanceof IVisitorWithAddresses) : "expected an instance of IVisitorWithAddresses";
    ((IVisitorWithAddresses) v).visitAddressOf(this);
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
    return (indexVal == -1) ? 1 : 2;
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
