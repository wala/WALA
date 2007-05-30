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

import java.util.Collection;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Exceptions;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 * 
 */
public class SSAMonitorInstruction extends SSAInstruction {
  private final int ref;

  private final boolean isEnter;

  SSAMonitorInstruction(int ref, boolean isEnter) {
    super();
    this.ref = ref;
    this.isEnter = isEnter;
  }

  @Override
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    // todo: check that this is the intended behavior. julian?
    return new SSAMonitorInstruction(uses == null || uses.length == 0 ? ref : uses[0], isEnter);
  }

  @Override
  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return "monitor " + getValueString(symbolTable, d, ref);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * @throws IllegalArgumentException  if v is null
   */
  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitMonitor(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfUses() {
    return 1;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    if (Assertions.verifyAssertions)
      Assertions._assert(j == 0);
    return ref;
  }

  @Override
  public int hashCode() {
    return ref * 6173 ^ 4423;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.Instruction#isPEI()
   */
  @Override
  public boolean isPEI() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Exceptions.getNullPointerException();
  }

  /**
   * @return Returns the ref.
   */
  public int getRef() {
    return ref;
  }

  public boolean isMonitorEnter() {
    return isEnter;
  }
}
