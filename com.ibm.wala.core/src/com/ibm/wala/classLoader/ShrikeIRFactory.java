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

import java.util.Collection;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.ShrikeCFG;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSABuilder;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.ShrikeIndirectionData;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.DeadAssignmentElimination;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaRuntimeException;

/**
 * An {@link IRFactory} that for methods that originate from Shrike.
 */
public class ShrikeIRFactory implements IRFactory<IBytecodeMethod> {

  public final static boolean buildLocalMap = true;

  public ControlFlowGraph makeCFG(final IBytecodeMethod method) {
    return ShrikeCFG.make(method);
  }

  @Override
  public IR makeIR(final IBytecodeMethod method, Context C, final SSAOptions options) throws IllegalArgumentException {

    if (method == null) {
      throw new IllegalArgumentException("null method");
    }
    com.ibm.wala.shrikeBT.IInstruction[] shrikeInstructions = null;
    try {
      shrikeInstructions = method.getInstructions();
    } catch (InvalidClassFileException e) {
      throw new WalaRuntimeException("bad method bytecodes", e);
    }
    final ShrikeCFG shrikeCFG = (ShrikeCFG) makeCFG(method);

    final SymbolTable symbolTable = new SymbolTable(method.getNumberOfParameters());
    final SSAInstruction[] newInstrs = new SSAInstruction[shrikeInstructions.length];

    final SSACFG newCfg = new SSACFG(method, shrikeCFG, newInstrs);

    return new IR(method, newInstrs, symbolTable, newCfg, options) {
      private final SSA2LocalMap localMap;

      private final ShrikeIndirectionData indirectionData;
      
      /**
       * Remove any phis that are dead assignments.
       * 
       * TODO: move this elsewhere?
       */
      private void eliminateDeadPhis() {
        DeadAssignmentElimination.perform(this);
      }
      private void pruneExceptionsForSafeArrayCreations() {
        DefUse du = new DefUse(this);
        for (int i = 0; i < newInstrs.length; i++) {
          SSAInstruction instr = newInstrs[i];
          if (instr instanceof SSANewInstruction) {
            SSANewInstruction newInstr = (SSANewInstruction) instr;
            if (newInstr.getConcreteType().isArrayType()) {
              boolean isSafe = true;
              final int[] params = new int[newInstr.getNumberOfUses()];
              for (int u = 0; u < newInstr.getNumberOfUses(); u++) {
                int vLength = newInstr.getUse(u);
                params[u] = vLength;
                isSafe &= (isNonNegativeConstant(vLength) || isDefdByArrayLength(vLength, du));
              }
              if (isSafe) {
                // newInstr is either obtained from
                //   JavaLanguage.JavaInstructionFactory#NewInstruction(int iindex, int result, NewSiteReference site, int[] params)
                // or
                //   JavaLanguage.JavaInstructionFactory#NewInstruction(int iindex, int result, NewSiteReference site)
                // , both provide anonymous subclasses of SSANewInstruction which differ
                // from SSANewInstruction only in the implementation of getExceptionTypes().
                // Hence, it is OK to just defining a new anonymous subclasses of SSANewInstruction, overriding getExceptionTypes().
                newInstrs[i] = new SSANewInstruction(newInstr.iindex, newInstr.getDef(), newInstr.getNewSite(), params) {
                  @Override
                  public Collection<TypeReference> getExceptionTypes() {
                    return JavaLanguage.getNewSafeArrayExceptions();
                  }
                };
              }
            }
          }
        }
      }
      private boolean isNonNegativeConstant(int vLength) {
        return symbolTable.isIntegerConstant(vLength) && symbolTable.getIntValue(vLength) >= 0;
      }
      private boolean isDefdByArrayLength(int vLength, DefUse du) {
        return du.getDef(vLength) instanceof SSAArrayLengthInstruction;
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

        indirectionData = builder.getIndirectionData();
        
        eliminateDeadPhis();
        pruneExceptionsForSafeArrayCreations();

        setupLocationMap();
      }

      @SuppressWarnings("unchecked")
      @Override
      protected ShrikeIndirectionData getIndirectionData() {
         return indirectionData;
      }
    };
  }

  @Override
  public boolean contextIsIrrelevant(IBytecodeMethod method) {
    // this factory always returns the same IR for a method
    return true;
  }
}
