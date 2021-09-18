/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ssa;

/** An instruction which unconditionally throws an exception */
public abstract class SSAThrowInstruction extends SSAAbstractThrowInstruction {

  protected SSAThrowInstruction(int iindex, int exception) {
    super(iindex, exception);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#copyForSSA(com.ibm.wala.ssa.SSAInstructionFactory, int[],
   *     int[])
   */
  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses)
      throws IllegalArgumentException {
    if (uses != null && uses.length != 1) {
      throw new IllegalArgumentException("if non-null, uses.length must be 1");
    }
    return insts.ThrowInstruction(iIndex(), uses == null ? getException() : uses[0]);
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor) */
  @Override
  public void visit(IVisitor v) throws NullPointerException {
    v.visitThrow(this);
  }
}
