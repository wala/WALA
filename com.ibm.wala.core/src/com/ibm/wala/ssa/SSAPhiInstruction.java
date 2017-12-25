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
package com.ibm.wala.ssa;

import java.util.Arrays;
import java.util.Iterator;

import com.ibm.wala.analysis.stackMachine.AbstractIntStackMachine;
import com.ibm.wala.cfg.ControlFlowGraph;

/**
 * A phi instruction in SSA form.
 * 
 * See any modern compiler textbook for the definition of phi and the nature of
 * SSA.
 * 
 * Note: In SSA {@link IR}s, these instructions do <em>not</em> appear in the
 * normal instruction array returned by IR.getInstructions(); instead these
 * instructions live in {@link ISSABasicBlock}.
 * 
 * <code>getUse(i)</code> corresponds to the value number from the
 * i<sup>th</sup> predecessor of the corresponding {@link ISSABasicBlock}
 * <code>b</code> in {@link ControlFlowGraph} <code>g</code>, where predecessor
 * order is the order of nodes returned by the {@link Iterator}
 * <code>g.getPredNodes(b)</code>.
 * 
 * Note: if getUse(i) returns {@link AbstractIntStackMachine}.TOP (that is, -1),
 * then that use represents an edge in the CFG which is infeasible in verifiable
 * bytecode.
 */
public class SSAPhiInstruction extends SSAInstruction {
  private final int result;

  private int[] params;

  public SSAPhiInstruction(int iindex, int result, int[] params) throws IllegalArgumentException {
    super(iindex);
    if (params == null) {
      throw new IllegalArgumentException("params is null");
    }
    this.result = result;
    this.params = params;
    if (params.length == 0) {
      throw new IllegalArgumentException("can't have phi with no params");
    }
    for (int p : params) {
      if (p == 0) {
        throw new IllegalArgumentException("zero is an invalid value number for a parameter to phi");
      }
    }
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) throws IllegalArgumentException {
    if (defs != null && defs.length == 0) {
      throw new IllegalArgumentException();
    }
    return insts.PhiInstruction(iindex, defs == null ? result : defs[0], uses == null ? params : uses);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    StringBuffer s = new StringBuffer();

    s.append(getValueString(symbolTable, result)).append(" = phi ");
    s.append(" ").append(getValueString(symbolTable, params[0]));
    for (int i = 1; i < params.length; i++) {
      s.append(",").append(getValueString(symbolTable, params[i]));
    }
    return s.toString();
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * @throws IllegalArgumentException if v is null
   */
  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitPhi(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public int getDef(int i) {
    if (i != 0) {
      throw new IllegalArgumentException("invalid i: " + i);
    }
    return result;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfUses() {
    return params.length;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) throws IllegalArgumentException {
    if (j >= params.length || j < 0) {
      throw new IllegalArgumentException("Bad use " + j);
    }
    return params[j];
  }

  /**
   * Clients should not call this.  only for SSA builders.
   * I hate this. Nuke it someday.
   */
  public void setValues(int[] i) {
    if (i == null || i.length < 1) {
      throw new IllegalArgumentException("illegal i: " + Arrays.toString(i));
    }
    this.params = i;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getValueString(SymbolTable, int)
   */
  @Override
  protected String getValueString(SymbolTable symbolTable, int valueNumber) {
    if (valueNumber == AbstractIntStackMachine.TOP) {
      return "TOP";
    } else {
      return super.getValueString(symbolTable, valueNumber);
    }
  }

  @Override
  public int hashCode() {
    return 7823 * result;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

 }
