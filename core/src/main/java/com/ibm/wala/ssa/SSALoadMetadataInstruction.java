/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
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

/**
 * An instruction that represents a reflective or meta-programming operation, like loadClass in Java
 */
public abstract class SSALoadMetadataInstruction extends SSAInstruction {

  private final int lval;

  /**
   * A representation of the meta-data itself. For a loadClass operation, this would be a {@link
   * TypeReference} representing the class object being manipulated
   */
  private final Object token;

  /**
   * The type of the thing that this meta-data represents. For a loadClass instruction, entityType
   * is java.lang.Class
   */
  private final TypeReference entityType;

  protected SSALoadMetadataInstruction(
      int iindex, int lval, TypeReference entityType, Object token) {
    super(iindex);
    this.lval = lval;
    this.token = token;
    this.entityType = entityType;
    if (token == null) {
      throw new IllegalArgumentException("null typeRef");
    }
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    if (defs != null && defs.length == 0) {
      throw new IllegalArgumentException("(defs != null) and (defs.length == 0)");
    }
    return insts.LoadMetadataInstruction(
        iIndex(), defs == null ? lval : defs[0], entityType, token);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, lval) + " = load_metadata: " + token + ", " + entityType;
  }

  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitLoadMetadata(this);
  }

  @Override
  public int hashCode() {
    return token.hashCode() * lval;
  }

  @Override
  public boolean isPEI() {
    return true;
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
  public boolean isFallThrough() {
    return true;
  }

  public Object getToken() {
    return token;
  }

  public TypeReference getType() {
    return entityType;
  }
}
