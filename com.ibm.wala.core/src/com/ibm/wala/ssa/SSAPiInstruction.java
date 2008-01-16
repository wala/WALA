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
 * You can build an IR with or without Pi instructions, depending on SSAOptions selected.
 * 
 * A Pi instruction is linked to its "cause" instruction, which is usually a conditional 
 * branch.
 * 
 * for example, the following pseudo-code
 * <verbatim>
 *     boolean condition = (x instanceof Integer);
 *     if (condition) {
 *        S1;
 *     else {
 *        S2;
 *     }
 * </verbatim>
 * 
 * could be translated roughly as follows:
 * 
 * <verbatim>
 *     boolean condition = (x instanceof Integer);
 *     LABEL1: if (condition) {
 *        x_1 = pi(x, LABEL1);
 *        S1;
 *     else {
 *        x_2 = pi(x, LABEL2);
 *        S2;
 *     }
 * </verbatim>
 * 
 * @author Julian Dolby
 * @author Stephen Fink
 *
 */
public class SSAPiInstruction extends SSAUnaryOpInstruction {
  private final SSAInstruction cause;

  private final int successorBlock;

  /**
   * @param s the successor block; this PI assignment happens on the transition between this basic block and
   * the successor block.
   */
  SSAPiInstruction(int result, int val, int s, SSAInstruction cause) {
    super(null, result, val);
    this.cause = cause;
    this.successorBlock = s;
  }

  @Override
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    if (defs != null && defs.length == 0) {
      throw new IllegalArgumentException("defs.length == 0");
    }
    if (uses != null && uses.length == 0) {
      throw new IllegalArgumentException("uses.length == 0");
    }
    return new SSAPiInstruction(defs == null ? result : defs[0], uses == null ? val : uses[0], successorBlock, cause);
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

  public SSAInstruction getCause() {
    return cause;
  }

  public int getVal() {
    return getUse(0);
  }

}
