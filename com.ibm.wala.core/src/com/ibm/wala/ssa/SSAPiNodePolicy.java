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

import java.util.List;

import com.ibm.wala.util.collections.Pair;

/**
 * The {@link SSABuilder} consults this as an oracle to decide how to insert {@link SSAPiInstruction}s
 */
public interface SSAPiNodePolicy {

  /**
   * Do we need to introduce a new name for some value immediately after a call?
   * 
   * If so, returns a pair consisting of the value number needing renaming, and the instruction which should be recorded as the
   * cause of the pi instruction
   * 
   * @param call the call instruction in question
   * @param symbolTable current state of the symbol table for the IR under construction
   * @return description of the necessary pi instruction, or null if no pi instruction is needed.
   */
  Pair<Integer, SSAInstruction> getPi(SSAAbstractInvokeInstruction call, SymbolTable symbolTable);

  /**
   * Do we need to introduce a new name for some value after deciding on an outcome for a conditional branch instruction?
   * 
   * If so, returns a pair consisting of the value number needing renaming, and the instruction which should be recorded as the
   * cause of the pi instruction
   * 
   * @param cond the conditional branch instruction in question
   * @param def1 the {@link SSAInstruction} that defs cond.getUse(0), or null if none
   * @param def2 the {@link SSAInstruction} that defs cond.getUse(1), or null if none
   * @param symbolTable current state of the symbol table for the IR under construction
   * @return description of the necessary pi instruction, or null if no pi instruction is needed.
   */
  Pair<Integer, SSAInstruction> getPi(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable);
  
  List<Pair<Integer, SSAInstruction>> getPis(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable);  

}
