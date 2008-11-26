/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

public class SSALoadClassInstruction extends SSAInstruction {

  private static final Collection<TypeReference> loadClassExceptions = Collections
      .singleton(TypeReference.JavaLangClassNotFoundException);

  private final int lval;

  private final TypeReference typeRef;

  public SSALoadClassInstruction(int lval, TypeReference typeRef) {
    this.lval = lval;
    this.typeRef = typeRef;
  }

  @Override
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    if (defs != null && defs.length == 0) {
      throw new IllegalArgumentException("(defs != null) and (defs.length == 0)");
    }
    return new SSALoadClassInstruction(defs == null ? lval : defs[0], typeRef);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, lval) + " = load_class: " + typeRef;
  }

  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitLoadClass(this);
  }

  @Override
  public int hashCode() {
    return typeRef.hashCode() * lval;
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return loadClassExceptions;
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
    if (Assertions.verifyAssertions) {
      Assertions._assert(i == 0);
    }
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

  public TypeReference getLoadedClass() {
    return typeRef;
  }
}
