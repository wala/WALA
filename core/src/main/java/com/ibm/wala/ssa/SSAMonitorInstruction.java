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

/** An instruction representing a monitorenter or monitorexit operation. */
public abstract class SSAMonitorInstruction extends SSAInstruction {
  /** The value number of the object being locked or unlocked */
  private final int ref;

  /** Does this instruction represent a monitorenter? */
  private final boolean isEnter;

  /**
   * @param ref The value number of the object being locked or unlocked
   * @param isEnter Does this instruction represent a monitorenter?
   */
  protected SSAMonitorInstruction(int iindex, int ref, boolean isEnter) {
    super(iindex);
    this.ref = ref;
    this.isEnter = isEnter;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    assert uses == null || uses.length == 1;
    return insts.MonitorInstruction(iIndex(), uses == null ? ref : uses[0], isEnter);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "monitor" + (isEnter ? "enter " : "exit ") + getValueString(symbolTable, ref);
  }

  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitMonitor(this);
  }

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getUse(int j) {
    assert j == 0;
    return ref;
  }

  @Override
  public int hashCode() {
    return ref * 6173 + 4423;
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  /** @return The value number of the object being locked or unlocked */
  public int getRef() {
    return ref;
  }

  /** Does this instruction represent a monitorenter? */
  public boolean isMonitorEnter() {
    return isEnter;
  }
}
