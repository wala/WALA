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
 * If we have the following code: <pre> S1: if (c op null) { ... } </pre>
 * 
 * replace it with: <pre> S1: if (c op null) { v2 = PI(c, S1) .... } </pre>
 * 
 * This renaming allows SSA-based analysis to reason about the nullness of v2 depending on the outcome of the branch.
 */
public class NullTestPiPolicy implements SSAPiNodePolicy {
  
  private final static NullTestPiPolicy singleton = new NullTestPiPolicy();
  
  public static NullTestPiPolicy createNullTestPiPolicy() {
    return singleton;
  }

  private NullTestPiPolicy() {}
  
  /*
   * @see com.ibm.wala.ssa.SSAPiNodePolicy#getPi(com.ibm.wala.ssa.SSAConditionalBranchInstruction,
   *      com.ibm.wala.ssa.SSAInstruction, com.ibm.wala.ssa.SSAInstruction, com.ibm.wala.ssa.SymbolTable)
   */
  @Override
  public Pair<Integer, SSAInstruction> getPi(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable) {
    if (symbolTable == null) {
      throw new IllegalArgumentException("null symbolTable");
    }
    if (cond == null) {
      throw new IllegalArgumentException("null cond");
    }
    if (symbolTable.isNullConstant(cond.getUse(1))) {
      return Pair.<Integer,SSAInstruction>make(cond.getUse(0), cond);
    }
    if (symbolTable.isNullConstant(cond.getUse(0))) {
      return Pair.<Integer,SSAInstruction>make(cond.getUse(1), cond);
    }
    return null;
  }

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
