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
package com.ibm.wala.cast.java.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;
import java.util.Collections;

/**
 * The CAst source language front end for Java has explicit support for lexically-enclosing objects,
 * rather than compiling them away into extra fields and access-control thwarting accessor methods
 * as is done in bytecode. This instruction represents a read of the object of the given type that
 * lexically encloses its use value.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class EnclosingObjectReference extends SSAInstruction {
  private final TypeReference type;

  private final int lval;

  public EnclosingObjectReference(int iindex, int lval, TypeReference type) {
    super(iindex);
    this.lval = lval;
    this.type = type;
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

  public TypeReference getEnclosingType() {
    return type;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstJavaInstructionFactory) insts)
        .EnclosingObjectReference(iIndex(), defs == null ? lval : defs[0], type);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, lval) + " = enclosing " + type.getName();
  }

  @Override
  public void visit(IVisitor v) {
    ((AstJavaInstructionVisitor) v).visitEnclosingObjectReference(this);
  }

  @Override
  public int hashCode() {
    return lval * type.hashCode();
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Collections.emptySet();
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
