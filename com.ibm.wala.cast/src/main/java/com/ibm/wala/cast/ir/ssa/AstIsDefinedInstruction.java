/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import java.util.Collection;
import java.util.Collections;

/**
 * IR instruction to check whether a field is defined on some object. The field is represented
 * either by a {@link FieldReference} or by a local value number.
 */
public class AstIsDefinedInstruction extends SSAInstruction {
  /** name of the field. If non-null, fieldVal should be -1. */
  private final FieldReference fieldRef;

  /** value number holding the field string. If non-negative, fieldRef should be null. */
  private final int fieldVal;

  /** the base pointer */
  private final int rval;

  /** gets 1 if the field is defined, 0 otherwise. */
  private final int lval;

  /**
   * This constructor should only be used from {@link
   * SSAInstruction#copyForSSA(SSAInstructionFactory, int[], int[])}
   */
  public AstIsDefinedInstruction(
      int iindex, int lval, int rval, int fieldVal, FieldReference fieldRef) {
    super(iindex);
    this.lval = lval;
    this.rval = rval;
    this.fieldVal = fieldVal;
    this.fieldRef = fieldRef;
  }

  public AstIsDefinedInstruction(int iindex, int lval, int rval, FieldReference fieldRef) {
    super(iindex);
    this.lval = lval;
    this.rval = rval;
    this.fieldVal = -1;
    this.fieldRef = fieldRef;
  }

  public AstIsDefinedInstruction(int iindex, int lval, int rval, int fieldVal) {
    super(iindex);
    this.lval = lval;
    this.rval = rval;
    this.fieldVal = fieldVal;
    this.fieldRef = null;
  }

  public AstIsDefinedInstruction(int iindex, int lval, int rval) {
    super(iindex);
    this.lval = lval;
    this.rval = rval;
    this.fieldVal = -1;
    this.fieldRef = null;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    assert fieldVal == -1 || fieldRef == null;

    return ((AstInstructionFactory) insts)
        .IsDefinedInstruction(
            iIndex(),
            (defs == null) ? lval : defs[0],
            (uses == null) ? rval : uses[0],
            (uses == null || fieldVal == -1) ? fieldVal : uses[1],
            fieldRef);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    if (fieldVal == -1 && fieldRef == null) {
      return getValueString(symbolTable, lval)
          + " = isDefined("
          + getValueString(symbolTable, rval)
          + ')';
    } else if (fieldVal == -1) {
      return getValueString(symbolTable, lval)
          + " = isDefined("
          + getValueString(symbolTable, rval)
          + ','
          + fieldRef.getName()
          + ')';
    } else if (fieldRef == null) {
      return getValueString(symbolTable, lval)
          + " = isDefined("
          + getValueString(symbolTable, rval)
          + ','
          + getValueString(symbolTable, fieldVal)
          + ')';
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  @Override
  public void visit(IVisitor v) {
    ((AstInstructionVisitor) v).visitIsDefined(this);
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Collections.emptySet();
  }

  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return lval;
  }

  @Override
  public int getDef(int i) {
    assert i == 0;

    return lval;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int getNumberOfUses() {
    return (fieldVal == -1) ? 1 : 2;
  }

  @Override
  public int getUse(int j) {
    if (j == 0) {
      return rval;
    } else if (j == 1 && fieldVal != -1) {
      return fieldVal;
    } else {
      Assertions.UNREACHABLE();
      return -1;
    }
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public int hashCode() {
    return 3077 * fieldVal * rval;
  }

  public FieldReference getFieldRef() {
    return fieldRef;
  }
}
