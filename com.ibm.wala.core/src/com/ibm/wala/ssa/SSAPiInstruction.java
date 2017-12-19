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

/**
 * A Pi instruction is a dummy assignment inserted at the tail of a basic block, in order
 * to get a new variable name to associate with some flow-insensitive dataflow fact.
 * You can build an {@link IR} with or without Pi instructions, depending on {@link SSAOptions} selected.
 * 
 * A Pi instruction is linked to its "cause" instruction, which is usually a conditional 
 * branch.
 * 
 * for example, the following pseudo-code
 * <p>
 * <pre>
 *     boolean condition = (x instanceof Integer);
 *     if (condition) {
 *        S1;
 *     else {
 *        S2;
 *     }
 * </pre>
 * 
 * could be translated roughly as follows:
 * 
 * <pre>
 *     boolean condition = (x instanceof Integer);
 *     LABEL1: if (condition) {
 *        x_1 = pi(x, LABEL1);
 *        S1;
 *     else {
 *        x_2 = pi(x, LABEL2);
 *        S2;
 *     }
 * </pre>
 */
public class SSAPiInstruction extends SSAUnaryOpInstruction {
  private final SSAInstruction cause;

  private final int successorBlock;
  
  private final int piBlock;

  /**
   * @param successorBlock the successor block; this PI assignment happens on the transition between this basic block and
   * the successor block.
   */
  public SSAPiInstruction(int iindex, int result, int val, int piBlock, int successorBlock, SSAInstruction cause) {
    super(iindex, null, result, val);
    this.cause = cause;
    this.successorBlock = successorBlock;
    this.piBlock = piBlock;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    assert defs == null || defs.length == 1;
    assert uses == null || uses.length == 1;
    return insts.PiInstruction(iindex, defs == null ? result : defs[0], uses == null ? val : uses[0], piBlock, successorBlock, cause);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result) + " = pi " + getValueString(symbolTable, val) + " for BB" + successorBlock + ", cause " + cause;
  }

  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitPi(this);
  }

  public int getSuccessor() {
    return successorBlock;
  }

  public int getPiBlock() {
    return piBlock;
  }
  
  public SSAInstruction getCause() {
    return cause;
  }

  public int getVal() {
    return getUse(0);
  }

}
