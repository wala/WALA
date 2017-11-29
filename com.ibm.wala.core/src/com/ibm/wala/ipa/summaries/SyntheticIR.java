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
package com.ibm.wala.ipa.summaries;

import java.util.Map;

import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAIndirectionData;
import com.ibm.wala.ssa.SSAIndirectionData.Name;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.debug.Assertions;

public class SyntheticIR extends IR {

  private final static boolean PARANOID = true;

  /**
   * Create an SSA form, induced over a list of instructions provided externally. This entrypoint is often used for, e.g., native
   * method models
   * 
   * @param method the method to construct SSA form for
   * @param context the governing context
   * @param instructions the SSA instructions which define the body of the method
   * @param constants a Map giving information on constant values for the symbol table
   * @throws AssertionError if method is null
   */
  public SyntheticIR(IMethod method, Context context, AbstractCFG cfg, SSAInstruction[] instructions, SSAOptions options,
      Map<Integer, ConstantValue> constants) throws AssertionError {
    super(method, instructions, makeSymbolTable(method, instructions, constants, cfg), new SSACFG(method, cfg, instructions),
        options);
    if (PARANOID) {
      repOK(instructions);
    }

    setupLocationMap();
  }

  /**
   * throw an assertion if the instruction array contains a phi instruction
   */
  private static void repOK(SSAInstruction[] instructions) {
    for (SSAInstruction s : instructions) {
      if (s instanceof SSAPhiInstruction) {
        Assertions.UNREACHABLE();
      }
      if (s instanceof SSAPiInstruction) {
        Assertions.UNREACHABLE();
      }
    }
  }

  /**
   * Set up the symbol table according to statements in the IR
   * 
   * @param constants Map: value number (Integer) -&gt; ConstantValue
   */
  private static SymbolTable makeSymbolTable(IMethod method, SSAInstruction[] instructions, Map<Integer, ConstantValue> constants,
      AbstractCFG cfg) {
    if (method == null) {
      throw new IllegalArgumentException("null method");
    }
    SymbolTable symbolTable = new SymbolTable(method.getNumberOfParameters());

    // simulate allocation of value numbers
    for (SSAInstruction s : instructions) {
      if (s != null) {
        updateForInstruction(constants, symbolTable, s);
      }
    }

    /**
     * In InducedCFGs, we have nulled out phi instructions from the instruction array ... so go back and retrieve them now.
     */
    if (cfg instanceof InducedCFG) {
      InducedCFG icfg = (InducedCFG) cfg;
      for (SSAPhiInstruction phi : icfg.getAllPhiInstructions()) {
        updateForInstruction(constants, symbolTable, phi);
      }
    }

    return symbolTable;
  }

  private static void updateForInstruction(Map<Integer, ConstantValue> constants, SymbolTable symbolTable, SSAInstruction s) {
    for (int j = 0; j < s.getNumberOfDefs(); j++) {
      symbolTable.ensureSymbol(s.getDef(j));
    }
    for (int j = 0; j < s.getNumberOfUses(); j++) {
      int vn = s.getUse(j);
      symbolTable.ensureSymbol(vn);
      if (constants != null && constants.containsKey(new Integer(vn)))
        symbolTable.setConstantValue(vn, constants.get(new Integer(vn)));
    }
  }

  /**
   * This returns "", as synthetic IRs have no line numbers right now.
   */
  @Override
  protected String instructionPosition(int instructionIndex) {
    return "";
  }

  /**
   * This returns null, as synthetic IRs have no local names right now.
   */
  @Override
  public SSA2LocalMap getLocalMap() {
    return null;
  }

  @Override
  protected SSAIndirectionData<Name> getIndirectionData() {
     return null;
  }
}
