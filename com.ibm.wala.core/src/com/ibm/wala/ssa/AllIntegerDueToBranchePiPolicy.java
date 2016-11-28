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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.util.collections.Pair;

/**
 * A policy, that adds pi nodes for all variables, that are used in a branch
 * instruction.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public class AllIntegerDueToBranchePiPolicy implements SSAPiNodePolicy {

  @Override
  public Pair<Integer, SSAInstruction> getPi(SSAAbstractInvokeInstruction call, SymbolTable symbolTable) {
    return null;
  }

  @Override
  public Pair<Integer, SSAInstruction> getPi(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable) {
    final List<Pair<Integer, SSAInstruction>> pis = this.getPis(cond, def1, def2, symbolTable);
    if (pis.size() == 0) {
      return null;
    } else if (pis.size() == 1) {
      return pis.get(0);
    } else {
      throw new IllegalArgumentException(
          "getPi was called but pi nodes should be inserted for more than one variable. Use getPis instead.");
    }
  }

  @Override
  public List<Pair<Integer, SSAInstruction>> getPis(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable) {
    if (cond.isIntegerComparison()) {
      final LinkedList<Pair<Integer, SSAInstruction>> result = new LinkedList<>();
      for (int i = 0; i < cond.getNumberOfUses(); i++) {
        result.add(Pair.make(cond.getUse(i), (SSAInstruction) cond));
      }
      return result;
    } else {
      return Collections.emptyList();
    }

  }
}
