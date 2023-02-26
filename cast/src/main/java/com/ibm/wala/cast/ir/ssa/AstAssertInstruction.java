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
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;

/**
 * An assert statement, as found in a variety of languages. It has a use which is the value being
 * asserted to be true. Additionally, there is flag which denotes whether the assertion is from a
 * specification (the usual case) or is an assertion introduced by "compilation" of whatever sort
 * (e.g. to add assertions regarding loop conditions needed by bounded model checking).
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class AstAssertInstruction extends SSAInstruction {
  private final int value;

  private final boolean fromSpecification;

  public AstAssertInstruction(int iindex, int value, boolean fromSpecification) {
    super(iindex);
    this.value = value;
    this.fromSpecification = fromSpecification;
  }

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getUse(int i) {
    assert i == 0;
    return value;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstInstructionFactory) insts)
        .AssertInstruction(iIndex(), uses == null ? value : uses[0], fromSpecification);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "assert "
        + getValueString(symbolTable, value)
        + " (fromSpec: "
        + fromSpecification
        + ')';
  }

  @Override
  public void visit(IVisitor v) {
    ((AstInstructionVisitor) v).visitAssert(this);
  }

  @Override
  public int hashCode() {
    return 2177 * value;
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return null;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  public boolean isFromSpecification() {
    return fromSpecification;
  }
}
