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

  private static final Collection<TypeReference> loadClassExceptions = Collections.singleton(TypeReference.JavaLangClassNotFoundException);

  private final int lval;

  private final TypeReference typeRef;

  public SSALoadClassInstruction(int lval, TypeReference typeRef) {
    this.lval = lval;
    this.typeRef = typeRef;
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new SSALoadClassInstruction(defs == null ? lval : defs[0], typeRef);
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return getValueString(symbolTable, d, lval) + " = load_class: " + typeRef;
  }

  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitLoadClass(this);
  }

  public int hashCode() {
    return typeRef.hashCode() * lval;
  }

  public boolean isPEI() {
    return true;
  }

  public Collection<TypeReference> getExceptionTypes() {
    return loadClassExceptions;
  }

  public int getDef() {
    return lval;
  }

  public int getDef(int i) {
    Assertions._assert(i == 0);
    return lval;
  }

  public int getNumberOfDefs() {
    return 1;
  }

  public boolean isFallThrough() {
    return true;
  }

  public TypeReference getLoadedClass() {
    return typeRef;
  }
}
