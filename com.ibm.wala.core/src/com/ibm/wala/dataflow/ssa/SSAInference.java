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

package com.ibm.wala.dataflow.ssa;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.fixedpoint.impl.NullaryOperator;
import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * This class performs intra-procedural propagation over an SSA form.
 * 
 * A client will subclass an {@link SSAInference} by providing factories that generate {@link IVariable}s corresponding to SSA value
 * numbers, and {@link AbstractOperator}s corresponding to SSA instructions. This class will set up a dataflow system induced by the
 * SSA def-use graph, and solve the system by iterating to a fixed point.
 * 
 * @see TypeInference for the canonical client of this machinery.
 */
public abstract class SSAInference<T extends IVariable<T>> extends DefaultFixedPointSolver<T> {
  static final boolean DEBUG = false;

  /**
   * The governing SSA form
   */
  private IR ir;

  /**
   * The governing symbol table
   */
  private SymbolTable symbolTable;

  /**
   * Dataflow variables, one for each value in the symbol table.
   */
  private IVariable[] vars;

  public interface OperatorFactory<T extends IVariable<T>> {
    /**
     * Get the dataflow operator induced by an instruction in SSA form.
     * 
     * @param instruction
     * @return dataflow operator for the instruction, or null if the instruction is not applicable to the dataflow system.
     */
    AbstractOperator<T> get(SSAInstruction instruction);
  }

  public interface VariableFactory {
    /**
     * Make the variable for a given value number.
     * 
     * @return a newly created dataflow variable, or null if not applicable.
     */
    public IVariable makeVariable(int valueNumber);
  }

  /**
   * initializer for SSA Inference equations.
   */
  protected void init(IR ir, VariableFactory varFactory, OperatorFactory<T> opFactory) {

    this.ir = ir;
    this.symbolTable = ir.getSymbolTable();

    createVariables(varFactory);
    createEquations(opFactory);
  }

  private void createEquations(OperatorFactory<T> opFactory) {
    SSAInstruction[] instructions = ir.getInstructions();
    for (SSAInstruction s : instructions) {
      makeEquationForInstruction(opFactory, s);
    }
    for (SSAInstruction s : Iterator2Iterable.make(ir.iteratePhis())) {
      makeEquationForInstruction(opFactory, s);
    }
    for (SSAInstruction s : Iterator2Iterable.make(ir.iteratePis())) {
      makeEquationForInstruction(opFactory, s);
    }
    for (SSAInstruction s : Iterator2Iterable.make(ir.iterateCatchInstructions())) {
      makeEquationForInstruction(opFactory, s);
    }
  }

  /**
   * Create a dataflow equation induced by a given instruction
   */
  private void makeEquationForInstruction(OperatorFactory<T> opFactory, SSAInstruction s) {
    if (s != null && s.hasDef()) {
      AbstractOperator<T> op = opFactory.get(s);
      if (op != null) {
        T def = getVariable(s.getDef());
        if (op instanceof NullaryOperator) {
          newStatement(def, (NullaryOperator<T>) op, false, false);
        } else {
          int n = s.getNumberOfUses();
          T[] uses = makeStmtRHS(n);
          for (int j = 0; j < n; j++) {
            if (s.getUse(j) > -1) {
              uses[j] = getVariable(s.getUse(j));
              assert uses[j] != null;
            }
          }
          newStatement(def, op, uses, false, false);
        }
      }
    }
  }

  /**
   * Create a dataflow variable for each value number
   */
  private void createVariables(VariableFactory factory) {
    vars = new IVariable[symbolTable.getMaxValueNumber() + 1];
    for (int i = 1; i < vars.length; i++) {
      vars[i] = factory.makeVariable(i);
    }

  }

  /**
   * @return the dataflow variable representing the value number, or null if none found.
   */
  @SuppressWarnings("unchecked")
  protected T getVariable(int valueNumber) {
    if (valueNumber < 0) {
      throw new IllegalArgumentException("Illegal valueNumber " + valueNumber);
    }
    if (DEBUG) {
      System.err.println(("getVariable for " + valueNumber + " returns " + vars[valueNumber]));
    }
    assert vars != null : "null vars array";
    return (T) vars[valueNumber];
  }

  /**
   * Return a string representation of the system
   * 
   * @return a string representation of the system
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("Type inference : \n");
    for (int i = 0; i < vars.length; i++) {
      result.append("v").append(i).append("  ").append(vars[i]).append("\n");
    }
    return result.toString();
  }
}
