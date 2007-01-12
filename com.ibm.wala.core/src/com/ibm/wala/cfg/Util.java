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
 * 
 *
 */
public class Util {

  public static SSAInstruction getLastInstruction(ControlFlowGraph G, IBasicBlock b) {
    return (SSAInstruction) G.getInstructions()[b.getLastInstructionIndex()];
  }

  public static boolean endsWithConditionalBranch(ControlFlowGraph G, IBasicBlock b) {
    return getLastInstruction(G, b) instanceof SSAConditionalBranchInstruction;
  }

  public static boolean endsWithSwitch(ControlFlowGraph G, IBasicBlock b) {
    return getLastInstruction(G, b) instanceof SSASwitchInstruction;
  }

  public static IBasicBlock getFallThruBlock(ControlFlowGraph G, IBasicBlock b) {
    return G.getBlockForInstruction(b.getLastInstructionIndex() + 1);
  }

  public static IBasicBlock getFalseSuccessor(ControlFlowGraph G, IBasicBlock b) {
    return getFallThruBlock(G, b);
  }

  public static IBasicBlock getTrueSuccessor(ControlFlowGraph G, IBasicBlock b) {
    IBasicBlock fs = getFalseSuccessor(G, b);
    for (Iterator ss = G.getSuccNodes(b); ss.hasNext();) {
      IBasicBlock s = (IBasicBlock) ss.next();
      if (s != fs)
        return s;
    }

    Assertions.UNREACHABLE();
    return null;
  }

  public static IBasicBlock resolveSwitch(ControlFlowGraph G, IBasicBlock b, int c) {
    SSASwitchInstruction s = (SSASwitchInstruction) getLastInstruction(G, b);
    int[] casesAndLabels = s.getCasesAndLabels();
    for (int i = 0; i < casesAndLabels.length; i += 2)
      if (casesAndLabels[i] == c)
        return G.getBlockForInstruction(casesAndLabels[i + 1]);

    return G.getBlockForInstruction(s.getDefault());
  }

  public static IBasicBlock resolveBranch(ControlFlowGraph G, IBasicBlock bb, int c1, int c2) {
    SSAConditionalBranchInstruction c = (SSAConditionalBranchInstruction) getLastInstruction(G, bb);
    switch ((ConditionalBranchInstruction.Operator)c.getOperator()) {
    case EQ:
      if (c1 == c2)
        return getTrueSuccessor(G, bb);
      else
        return getFalseSuccessor(G, bb);
    case NE:
      if (c1 != c2)
        return getTrueSuccessor(G, bb);
      else
        return getFalseSuccessor(G, bb);
    case LT:
      if (c1 < c2)
        return getTrueSuccessor(G, bb);
      else
        return getFalseSuccessor(G, bb);
    case GE:
      if (c1 >= c2)
        return getTrueSuccessor(G, bb);
      else
        return getFalseSuccessor(G, bb);
    case GT:
      if (c1 > c2)
        return getTrueSuccessor(G, bb);
      else
        return getFalseSuccessor(G, bb);
    case LE:
      if (c1 <= c2)
        return getTrueSuccessor(G, bb);
      else
        return getFalseSuccessor(G, bb);
    }

    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#whichPred(com.ibm.wala.cfg.IBasicBlock,
   *      com.ibm.wala.cfg.IBasicBlock)
   */
  public static int whichPred(ControlFlowGraph cfg, IBasicBlock a, IBasicBlock b) {
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
