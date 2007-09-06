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
package com.ibm.wala.cfg;

import java.util.Iterator;

import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.util.debug.Assertions;

/**
 * Convenience methods for navigating a CFG.
 */
public class Util {

  public static SSAInstruction getLastInstruction(ControlFlowGraph G, IBasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    return (SSAInstruction) G.getInstructions()[b.getLastInstructionIndex()];
  }

  public static boolean endsWithConditionalBranch(ControlFlowGraph G, IBasicBlock b) {
    return getLastInstruction(G, b) instanceof SSAConditionalBranchInstruction;
  }

  public static boolean endsWithSwitch(ControlFlowGraph G, IBasicBlock b) {
    return getLastInstruction(G, b) instanceof SSASwitchInstruction;
  }

  public static <T extends IBasicBlock> T getFallThruBlock(ControlFlowGraph<T> G, T b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    return G.getBlockForInstruction(b.getLastInstructionIndex() + 1);
  }

  /**
   * Given that b ends with a conditional branch, return the basic block to
   * which control transfers if the branch is not taken.
   */
  public static <T extends IBasicBlock> T getNotTakenSuccessor(ControlFlowGraph<T> G, T b) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    if (!endsWithConditionalBranch(G, b)) {
      throw new IllegalArgumentException(b.toString() + " does not end with a conditional branch");
    }
    return getFallThruBlock(G, b);
  }

  /**
   * Given that b ends with a conditional branch, return the basic block to
   * which control transfers if the branch is taken.
   */
  public static <T extends IBasicBlock> T getTakenSuccessor(ControlFlowGraph<T> G, T b) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    if (!endsWithConditionalBranch(G, b)) {
      throw new IllegalArgumentException(b.toString() + " does not end with a conditional branch");
    }
    T fs = getNotTakenSuccessor(G, b);
    for (Iterator<? extends T> ss = G.getSuccNodes(b); ss.hasNext();) {
      T s = ss.next();
      if (s != fs)
        return s;
    }

    // under pathological conditions, b may have exactly one successor (in other
    // words, the
    // branch is irrelevant
    return fs;
  }

  /**
   * When the tested value of the switch statement in b has value c, which basic
   * block does control transfer to.
   */
  public static IBasicBlock resolveSwitch(ControlFlowGraph G, IBasicBlock b, int c) {
    Assertions._assert(endsWithSwitch(G, b));
    SSASwitchInstruction s = (SSASwitchInstruction) getLastInstruction(G, b);
    int[] casesAndLabels = s.getCasesAndLabels();
    for (int i = 0; i < casesAndLabels.length; i += 2)
      if (casesAndLabels[i] == c)
        return G.getBlockForInstruction(casesAndLabels[i + 1]);

    return G.getBlockForInstruction(s.getDefault());
  }

  public static boolean isSwitchDefault(ControlFlowGraph G, IBasicBlock b, IBasicBlock s) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    Assertions._assert(endsWithSwitch(G, b));
    SSASwitchInstruction sw = (SSASwitchInstruction) getLastInstruction(G, b);
    assert G.getBlockForInstruction(sw.getDefault()) != null;
    return G.getBlockForInstruction(sw.getDefault()).equals(s);
  }

  /**
   * When a switch statement at the end of block b transfers control to block s,
   * which case was taken? TODO: Is this correct? Can't we have multiple cases
   * that apply? Check on this.
   */
  public static int getSwitchLabel(ControlFlowGraph G, IBasicBlock b, IBasicBlock s) {
    Assertions._assert(endsWithSwitch(G, b));
    SSASwitchInstruction sw = (SSASwitchInstruction) getLastInstruction(G, b);
    int[] casesAndLabels = sw.getCasesAndLabels();
    for (int i = 0; i < casesAndLabels.length; i += 2) {
      if (G.getBlockForInstruction(casesAndLabels[i + 1]).equals(s)) {
        return casesAndLabels[i];
      }
    }

    Assertions.UNREACHABLE();
    return -1;
  }

  public static <T extends IBasicBlock> T resolveBranch(ControlFlowGraph<T> G, T bb, int c1, int c2) {
    SSAConditionalBranchInstruction c = (SSAConditionalBranchInstruction) getLastInstruction(G, bb);
    switch ((ConditionalBranchInstruction.Operator) c.getOperator()) {
    case EQ:
      if (c1 == c2)
        return getTakenSuccessor(G, bb);
      else
        return getNotTakenSuccessor(G, bb);
    case NE:
      if (c1 != c2)
        return getTakenSuccessor(G, bb);
      else
        return getNotTakenSuccessor(G, bb);
    case LT:
      if (c1 < c2)
        return getTakenSuccessor(G, bb);
      else
        return getNotTakenSuccessor(G, bb);
    case GE:
      if (c1 >= c2)
        return getTakenSuccessor(G, bb);
      else
        return getNotTakenSuccessor(G, bb);
    case GT:
      if (c1 > c2)
        return getTakenSuccessor(G, bb);
      else
        return getNotTakenSuccessor(G, bb);
    case LE:
      if (c1 <= c2)
        return getTakenSuccessor(G, bb);
      else
        return getNotTakenSuccessor(G, bb);
    }

    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * Given that a is a predecessor of b in the cfg ..
   * 
   * When we enumerate the predecessors of b in order, which is the first index
   * in this order in which a appears? Note that this order corresponds to the
   * order of operands in a phi instruction.
   */
  public static <T extends IBasicBlock> int whichPred(ControlFlowGraph<T> cfg, T a, T b) {
    if (cfg == null) {
      throw new IllegalArgumentException("cfg is null");
    }
    if (a == null) {
      throw new IllegalArgumentException("a is null");
    }
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    int i = 0;
    for (Iterator it = cfg.getPredNodes(b); it.hasNext();) {
      if (it.next().equals(a)) {
        return i;
      }
      i++;
    }
    Assertions.UNREACHABLE("Invalid: a must be a predecessor of b! " + a + " " + b);
    return -1;
  }
}
