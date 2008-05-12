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
package com.ibm.wala.classLoader;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.ShrikeCFG;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSABuilder;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.DeadAssignmentElimination;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author Julian Dolby
 */
public class ShrikeIRFactory implements IRFactory<ShrikeCTMethod> {

  public final static boolean buildLocalMap = true;

  public ControlFlowGraph makeCFG(final ShrikeCTMethod method, Context C) {
    return new ShrikeCFG(method);
  }

  public IR makeIR(final ShrikeCTMethod method, Context C, final SSAOptions options) throws IllegalArgumentException {

    com.ibm.wala.shrikeBT.Instruction[] shrikeInstructions;
    try {
      shrikeInstructions = method.getInstructions();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      shrikeInstructions = null;
    }
    final ShrikeCFG shrikeCFG = (ShrikeCFG) makeCFG(method, C);

    final SymbolTable symbolTable = new SymbolTable(method.getNumberOfParameters());
    final SSAInstruction[] newInstrs = new SSAInstruction[shrikeInstructions.length];

    final SSACFG newCfg = new SSACFG(method, shrikeCFG, newInstrs);

    return new IR(method, newInstrs, symbolTable, newCfg, options) {
      private final SSA2LocalMap localMap;

      /**
       * Remove any phis that are dead assignments.
       * 
       * TODO: move this elsewhere?
       */
      private void eliminateDeadPhis() {
        DeadAssignmentElimination.perform(this);
      }

      @Override
      protected String instructionPosition(int instructionIndex) {
        try {
          int bcIndex = method.getBytecodeIndex(instructionIndex);
          int lineNumber = method.getLineNumber(bcIndex);

          if (lineNumber == -1) {
            return "";
          } else {
            return "(line " + lineNumber + ")";
          }
        } catch (InvalidClassFileException e) {
          return "";
        }
      }

      @Override
      public SSA2LocalMap getLocalMap() {
        return localMap;
      }

      {
        SSABuilder builder = SSABuilder.make(method, newCfg, shrikeCFG, newInstrs, symbolTable, buildLocalMap, options
            .getPiNodePolicy());
        builder.build();
        if (buildLocalMap)
          localMap = builder.getLocalMap();
        else
          localMap = null;

        eliminateDeadPhis();

        setupLocationMap();
      }
    };
  }

  public boolean contextIsIrrelevant(ShrikeCTMethod method) {
    // this factory always returns the same IR for a method
    return true;
  }
}
