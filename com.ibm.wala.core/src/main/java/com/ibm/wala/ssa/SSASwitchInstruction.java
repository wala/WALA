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

import com.ibm.wala.util.intset.IntIterator;

/** SSA instruction representing a switch statement. */
public class SSASwitchInstruction extends SSAInstruction {
  private final int val;

  private final int defaultLabel;

  private final int[] casesAndLabels;

  /**
   * The labels in casesAndLabels represent <em>instruction indices</em> in the IR that each switch
   * case branches to.
   */
  public SSASwitchInstruction(int iindex, int val, int defaultLabel, int[] casesAndLabels) {
    super(iindex);
    this.val = val;
    this.defaultLabel = defaultLabel;
    this.casesAndLabels = casesAndLabels;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    assert uses == null || uses.length == 1;
    for (int i = 1; i < casesAndLabels.length; i += 2) {
      assert casesAndLabels[i] != iIndex() : "do not branch to self: " + this;
    }
    return insts.SwitchInstruction(
        iIndex(), uses == null ? val : uses[0], defaultLabel, casesAndLabels);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    StringBuilder result = new StringBuilder(iIndex() + ": switch ");
    result.append(getValueString(symbolTable, val));
    result.append(" [");
    for (int i = 0; i < casesAndLabels.length - 1; i++) {
      result.append(casesAndLabels[i]);
      i++;
      result.append("->");
      result.append(casesAndLabels[i]);
      if (i < casesAndLabels.length - 2) {
        result.append(',');
      }
    }
    result.append("] default: ").append(defaultLabel);
    return result.toString();
  }

  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitSwitch(this);
  }

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getUse(int j) {
    assert j <= 1;
    return val;
  }

  // public int[] getTargets() {
  // // TODO Auto-generated method stub
  // Assertions.UNREACHABLE();
  // return null;
  // }

  public int getTarget(int caseValue) {
    for (int i = 0; i < casesAndLabels.length; i += 2)
      if (caseValue == casesAndLabels[i]) return casesAndLabels[i + 1];

    return defaultLabel;
  }

  public int getDefault() {
    return defaultLabel;
  }

  public int[] getCasesAndLabels() {
    return casesAndLabels;
  }

  public IntIterator iterateLabels() {
    return new IntIterator() {
      private int i = 0;

      @Override
      public boolean hasNext() {
        return i < casesAndLabels.length;
      }

      @Override
      public int next() {
        int v = casesAndLabels[i];
        i += 2;
        return v;
      }
    };
  }

  @Override
  public int hashCode() {
    return val * 1663 + 3499;
  }

  @Override
  public boolean isFallThrough() {
    return false;
  }
}
