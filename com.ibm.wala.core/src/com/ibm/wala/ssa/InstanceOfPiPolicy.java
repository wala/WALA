/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.util.collections.Pair;

/**
 * A pi node policy with the following rule:
 * 
 * If we have the following code:
 *
 * <pre> S1: c = v1 instanceof T S2: if (c == 0) { ... } </pre>
 * 
 * replace it with:
 *
 * <pre> S1: c = v1 instanceof T S2: if (c == 0) { v2 = PI(v1, S1) .... } </pre>
 * 
 * The same pattern holds if the test is c == 1. This renaming allows SSA-based analysis to reason about the type of v2 depending on
 * the outcome of the branch.
 */
public class InstanceOfPiPolicy implements SSAPiNodePolicy {

  private final static InstanceOfPiPolicy singleton = new InstanceOfPiPolicy();

  public static InstanceOfPiPolicy createInstanceOfPiPolicy() {
    return singleton;
  }

  private InstanceOfPiPolicy() {
  }

  /*
   * @see com.ibm.wala.ssa.SSAPiNodePolicy#getPi(com.ibm.wala.ssa.SSAConditionalBranchInstruction, com.ibm.wala.ssa.SSAInstruction,
   * com.ibm.wala.ssa.SSAInstruction, com.ibm.wala.ssa.SymbolTable)
   */
  @Override
  public Pair<Integer, SSAInstruction> getPi(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable) {
    if (def1 instanceof SSAInstanceofInstruction) {
      if (symbolTable.isBooleanOrZeroOneConstant(cond.getUse(1))) {
        return Pair.make(def1.getUse(0), def1);
      }
    }
    if (def2 instanceof SSAInstanceofInstruction) {
      if (symbolTable.isBooleanOrZeroOneConstant(cond.getUse(0))) {
        return Pair.make(def2.getUse(0), def2);
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  @Override
  public int hashCode() {
    return 12;
  }

  /* 
   * @see com.ibm.wala.ssa.SSAPiNodePolicy#getPi(com.ibm.wala.ssa.SSAAbstractInvokeInstruction, com.ibm.wala.ssa.SymbolTable)
   */
  @Override
  public Pair<Integer, SSAInstruction> getPi(SSAAbstractInvokeInstruction call, SymbolTable symbolTable) {
    return null;
  }

  @Override
  public List<Pair<Integer, SSAInstruction>> getPis(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable) {
    LinkedList<Pair<Integer, SSAInstruction>> result = new LinkedList<>();
    result.add(getPi(cond, def1, def2, symbolTable));
    return result;
  }

}
