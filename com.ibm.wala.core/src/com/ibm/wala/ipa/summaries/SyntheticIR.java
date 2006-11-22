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
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.warnings.WarningSet;

public class SyntheticIR extends IR {

  /**
   * Create an SSA form, induced over a list of instructions provided
   * externally. This entrypoint is often used for, e.g., native method models
   * 
   * @param method
   *          the method to construct SSA form for
   * @param context
   *          the govening context
   * @param instructions
   *          the SSA instructions which define the body of the method
   * @param constants
   *          a Map giving information on constant values for the symbol table
   * @param warnings
   *          an object to track analysis warnings with
   */
  public SyntheticIR(IMethod method, Context context, AbstractCFG cfg, SSAInstruction[] instructions, SSAOptions options,
      Map<Integer, ConstantValue> constants, WarningSet warnings) {
    super(method, instructions, makeSymbolTable(method, instructions, constants), new SSACFG(method, cfg, instructions, warnings),
        options);

    setupLocationMap();
  }

  /**
   * Set up the symbol table according to statements in the IR
   * 
   * @param constants
   *          Map: valune number (Integer) -> ConstantValue
   */
  private static SymbolTable makeSymbolTable(IMethod method, SSAInstruction[] instructions, Map<Integer, ConstantValue> constants) {
    SymbolTable symbolTable = new SymbolTable(method.getNumberOfParameters());

    // simulate allocation of value numbers
    for (int i = 0; i < instructions.length; i++) {
      if (instructions[i] != null) {
        for (int j = 0; j < instructions[i].getNumberOfDefs(); j++) {
          symbolTable.ensureSymbol(instructions[i].getDef(j));
        }
        for (int j = 0; j < instructions[i].getNumberOfUses(); j++) {
          int vn = instructions[i].getUse(j);
          symbolTable.ensureSymbol(vn);
          if (constants != null && constants.containsKey(new Integer(vn)))
            symbolTable.setConstantValue(vn, constants.get(new Integer(vn)));
        }
      }
    }

    return symbolTable;
  }

  /**
   *  This returns null, as synthetic IRs have no local names right now.
   */
  public SSA2LocalMap getLocalMap() {
    return null;
  }
}
