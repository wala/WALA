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
  final private int[] label;

  protected GotoInstruction(int label) {
    super(OP_goto);
    int[] l = { label };
    this.label = l;
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

  @Override
  public boolean isFallThrough() {
    return false;
  }

  @Override
  public int[] getBranchTargets() {
    return label;
  }

  public int getLabel() {
    return label[0];
  }

  @Override
  public IInstruction redirectTargets(int[] targetMap) throws IllegalArgumentException {
    if (targetMap == null) {
      throw new IllegalArgumentException("targetMap is null");
    }
    try {
      return make(targetMap[label[0]]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Illegal target map", e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof GotoInstruction) {
      GotoInstruction i = (GotoInstruction) o;
      return i.label == label;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return label[0] * 1348091 + 18301;
  }

  @Override
  public String toString() {
    return "Goto(" + getLabel() + ")";
  }

  @Override
  public void visit(IInstruction.Visitor v) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitGoto(this);
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
