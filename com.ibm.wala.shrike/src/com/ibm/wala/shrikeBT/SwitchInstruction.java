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

import java.util.Arrays;

/**
 * This instruction represents all forms of switch instructions.
 */
public final class SwitchInstruction extends Instruction {
  final private int[] casesAndLabels;

  final private int defaultLabel;

  protected SwitchInstruction(short opcode, int[] casesAndLabels, int defaultLabel) {
    super(opcode);
    this.casesAndLabels = casesAndLabels;
    this.defaultLabel = defaultLabel;
  }

  /**
   * @return the label which is branched to if none of the cases match
   */
  public int getDefaultLabel() {
    return defaultLabel;
  }

  /**
   * @return an array of flattened (case, label) pairs, sorted in increasing order by case
   */
  public int[] getCasesAndLabels() {
    return casesAndLabels;
  }

  /**
   * Make a switch instruction.
   * 
   * @param casesAndLabels an array of flattened (case, label) pairs, sorted in increasing order by case
   * @param defaultLabel the default label to branch to if no cases match
   * @throws IllegalArgumentException if casesAndLabels is null
   */
  public static SwitchInstruction make(int[] casesAndLabels, int defaultLabel) {
    if (casesAndLabels == null) {
      throw new IllegalArgumentException("casesAndLabels is null");
    }
    short opcode = OP_tableswitch;

    for (int i = 2; i < casesAndLabels.length; i += 2) {
      int curCase = casesAndLabels[i];
      int lastCase = casesAndLabels[i - 2];
      if (curCase <= lastCase) {
        throw new IllegalArgumentException("Cases and labels array must be sorted by case");
      }
      if (curCase != lastCase + 1) {
        opcode = OP_lookupswitch;
      }
    }

    if (casesAndLabels.length == 0) {
      opcode = OP_lookupswitch;
    }

    return new SwitchInstruction(opcode, casesAndLabels, defaultLabel);
  }

  @Override
  public boolean isFallThrough() {
    return false;
  }

  @Override
  public int[] getBranchTargets() {
    int[] r = new int[casesAndLabels.length / 2 + 1];
    r[0] = defaultLabel;
    for (int i = 1; i < r.length; i++) {
      r[i] = casesAndLabels[(i - 1) * 2 + 1];
    }
    return r;
  }

  @Override
  public IInstruction redirectTargets(int[] targetMap) throws IllegalArgumentException {
    if (targetMap == null) {
      throw new IllegalArgumentException("targetMap is null");
    }
    try {
      int[] cs = new int[casesAndLabels.length];
      for (int i = 0; i < cs.length; i += 2) {
        cs[i] = casesAndLabels[i];
        cs[i + 1] = targetMap[casesAndLabels[i + 1]];
      }
      return make(cs, targetMap[defaultLabel]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Illegal target map", e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SwitchInstruction) {
      SwitchInstruction i = (SwitchInstruction) o;
      return i.defaultLabel == defaultLabel && Arrays.equals(i.casesAndLabels, casesAndLabels);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int h = defaultLabel * 1348091 + 111311;
    for (int i = 0; i < casesAndLabels.length; i++) {
      h += (i * 9301 + 38101) * casesAndLabels[i];
    }
    return h;
  }

  @Override
  public int getPoppedCount() {
    return 1;
  }

  @Override
  public String toString() {
    StringBuffer b = new StringBuffer("Switch(");
    b.append(defaultLabel);
    for (int casesAndLabel : casesAndLabels) {
      b.append(',');
      b.append(casesAndLabel);
    }
    b.append(")");
    return b.toString();
  }

  @Override
  public void visit(IInstruction.Visitor v) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitSwitch(this);
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
