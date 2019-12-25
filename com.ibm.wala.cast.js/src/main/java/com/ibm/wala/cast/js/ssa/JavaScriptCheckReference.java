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
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;
import java.util.Collections;

/**
 * Checks if a reference is null or undefined, and if so, throws a ReferenceError. Otherwise, it's a
 * no-op.
 */
public class JavaScriptCheckReference extends SSAInstruction {
  private final int ref;

  public JavaScriptCheckReference(int iindex, int ref) {
    super(iindex);
    this.ref = ref;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((JSInstructionFactory) insts).CheckReference(iIndex(), uses == null ? ref : uses[0]);
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Collections.singleton(JavaScriptTypes.ReferenceError);
  }

  @Override
  public int hashCode() {
    return 87621 * ref;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "check " + getValueString(symbolTable, ref);
  }

  @Override
  public void visit(IVisitor v) {
    ((JSInstructionVisitor) v).visitCheckRef(this);
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getUse(int i) {
    assert i == 0;
    return ref;
  }
}
