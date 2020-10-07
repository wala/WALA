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

import com.ibm.wala.ssa.SSAAbstractBinaryInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;
import java.util.Collections;

/**
 * This instruction represents iterating through the properties of its receiver object. The use
 * represents an object, and the l-value represents a boolean indicating whether the object has more
 * properties.
 *
 * <p>Iterating across the fields or properties of a given object is a common idiom in scripting
 * languages, which is why the IR has first-class support for it.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class EachElementHasNextInstruction extends SSAAbstractBinaryInstruction {

  public EachElementHasNextInstruction(int iindex, int lValue, int objectRef, int previousPropVal) {
    super(iindex, lValue, objectRef, previousPropVal);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstInstructionFactory) insts)
        .EachElementHasNextInstruction(
            iIndex(),
            (defs == null) ? getDef(0) : defs[0],
            (uses == null) ? getUse(0) : uses[0],
            (uses == null) ? getUse(1) : uses[1]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, getDef(0))
        + " = has next property: "
        + getValueString(symbolTable, getUse(0));
  }

  @Override
  public void visit(IVisitor v) {
    ((AstInstructionVisitor) v).visitEachElementHasNext(this);
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
