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
import com.ibm.wala.ipa.cha.IClassHierarchy;
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
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author Julian Dolby
 * 
 */
public class ShrikeIRFactory implements IRFactory {

  public final static boolean buildLocalMap = true;

  /*
   * @see com.ibm.wala.ssa.IRFactory#makeCFG(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.cha.IClassHierarchy,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public ControlFlowGraph makeCFG(final IMethod method, Context C, final IClassHierarchy cha, final WarningSet warnings) {
    return new ShrikeCFG((ShrikeCTMethod) method, warnings, cha);
  }

  /*
   * @see com.ibm.wala.ssa.IRFactory#makeIR(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.cha.IClassHierarchy, com.ibm.wala.ssa.SSAOptions,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public IR makeIR(final IMethod method, Context C, final IClassHierarchy cha, final SSAOptions options, final WarningSet warnings)
      throws IllegalArgumentException {

    if (!(method instanceof ShrikeCTMethod)) {
      throw new IllegalArgumentException("method must be a ShrikeCTMethod");
    }

    com.ibm.wala.shrikeBT.Instruction[] shrikeInstructions;
    try {
      shrikeInstructions = ((ShrikeCTMethod) method).getInstructions();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      shrikeInstructions = null;
    }
    final ShrikeCFG shrikeCFG = (ShrikeCFG) makeCFG(method, C, cha, warnings);

    final SymbolTable symbolTable = new SymbolTable(method.getNumberOfParameters());
    final SSAInstruction[] newInstrs = new SSAInstruction[shrikeInstructions.length];

    final SSACFG newCfg = new SSACFG(method, shrikeCFG, newInstrs, warnings);

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
          int bcIndex = ((ShrikeCTMethod) method).getBytecodeIndex(instructionIndex);
          int lineNumber = ((ShrikeCTMethod) method).getLineNumber(bcIndex);

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
        SSABuilder builder = new SSABuilder((ShrikeCTMethod) method, cha, newCfg, shrikeCFG, newInstrs, symbolTable, buildLocalMap,
            options.getUsePiNodes(), warnings);
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
}
