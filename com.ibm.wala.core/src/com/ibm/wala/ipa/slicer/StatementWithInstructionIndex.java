/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;

/**
 * A {@link Statement} which carries an instruction index, representing the index of an {@link
 * SSAInstruction} in the IR instruction array.
 */
public abstract class StatementWithInstructionIndex extends Statement {

  private final int instructionIndex;

  protected StatementWithInstructionIndex(CGNode node, int instructionIndex) {
    super(node);
    this.instructionIndex = instructionIndex;
  }

  public int getInstructionIndex() {
    return instructionIndex;
  }

  public SSAInstruction getInstruction() {
    return getNode().getIR().getInstructions()[instructionIndex];
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + instructionIndex;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    final StatementWithInstructionIndex other = (StatementWithInstructionIndex) obj;
    if (instructionIndex != other.instructionIndex) return false;
    return true;
  }

  @Override
  public String toString() {
    return super.toString() + '[' + getInstructionIndex() + ']' + getInstruction();
  }
}
