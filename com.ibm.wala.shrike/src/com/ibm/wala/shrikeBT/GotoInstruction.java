/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT;

/**
 * This class represents goto and goto_w instructions.
 */
public final class GotoInstruction extends Instruction {
  private int[] label;

  protected GotoInstruction(int label) {
    int[] l = { label };
    this.label = l;
    this.opcode = OP_goto;
  }

  private final static GotoInstruction[] preallocated = preallocate();

  private static GotoInstruction[] preallocate() {
    GotoInstruction[] r = new GotoInstruction[256];
    for (int i = 0; i < r.length; i++) {
      r[i] = new GotoInstruction(i);
    }
    return r;
  }

  public static GotoInstruction make(int label) {
    if (0 <= label && label < preallocated.length) {
      return preallocated[label];
    } else {
      return new GotoInstruction(label);
    }
  }

  public boolean isFallThrough() {
    return false;
  }

  public int[] getBranchTargets() {
    return label;
  }

  public int getLabel() {
    return label[0];
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.shrikeBT.Instruction#redirectTargets(int[])
   */
  public Instruction redirectTargets(int[] targetMap) throws IllegalArgumentException {
    try {
      return make(targetMap[label[0]]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Illegal target map", e);
    }
  }

  public boolean equals(Object o) {
    if (o instanceof GotoInstruction) {
      GotoInstruction i = (GotoInstruction) o;
      return i.label == label;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return label[0] * 1348091 + 18301;
  }

  public String toString() {
    return "Goto(" + getLabel() + ")";
  }

  public void visit(Visitor v) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitGoto(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.domo.cfg.IInstruction#isPEI()
   */
  public boolean isPEI() {
    return false;
  }
}