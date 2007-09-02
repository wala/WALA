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
package com.ibm.wala.viz;

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.wala.cfg.CFGSanitizer;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.util.StringStuff;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeDecorator;
import com.ibm.wala.util.warnings.WalaException;

/**
 * utilities for integrating with ghostview
 * 
 * @author sfink
 */
public class GhostviewUtil {

  /**
   * spawn a process to ghostview a WALA IR
   * 
   * @return a handle to the ghostview process
   */
  public static Process ghostviewIR(IClassHierarchy cha, IR ir, String psFile, String dotFile, String dotExe, String gvExe)
      throws WalaException {
    return ghostviewIR(cha, ir, psFile, dotFile, dotExe, gvExe, null);
  }

  /**
   * spawn a process to ghostview a WALA IR
   * 
   * @return a handle to the ghostview process
   * @throws IllegalArgumentException
   *             if ir is null
   */
  public static Process ghostviewIR(IClassHierarchy cha, IR ir, String psFile, String dotFile, String dotExe, String gvExe,
      NodeDecorator annotations) throws WalaException {

    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    Graph<? extends ISSABasicBlock> g = ir.getControlFlowGraph();

    NodeDecorator labels = makeIRDecorator(ir);
    if (annotations != null) {
      labels = new ConcatenatingNodeDecorator(annotations, labels);
    }

    g = CFGSanitizer.sanitize(ir, cha);

    DotUtil.dotify(g, labels, dotFile, psFile, dotExe);

    return GVUtil.launchGV(psFile, gvExe);
  }

  public static NodeDecorator makeIRDecorator(IR ir) {
    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    final HashMap<BasicBlock, String> labelMap = HashMapFactory.make();
    for (Iterator it = ir.getControlFlowGraph().iterator(); it.hasNext();) {
      SSACFG.BasicBlock bb = (SSACFG.BasicBlock) it.next();
      labelMap.put(bb, getNodeLabel(ir, bb));
    }
    NodeDecorator labels = new NodeDecorator() {
      public String getLabel(Object o) {
        return labelMap.get(o);
      }
    };
    return labels;
  }

  /**
   * @author sfink
   * 
   * A node decorator which concatenates the labels from two other node
   * decorators
   */
  private final static class ConcatenatingNodeDecorator implements NodeDecorator {

    private final NodeDecorator A;

    private final NodeDecorator B;

    ConcatenatingNodeDecorator(NodeDecorator A, NodeDecorator B) {
      this.A = A;
      this.B = B;
    }

    public String getLabel(Object o) throws WalaException {
      return A.getLabel(o) + B.getLabel(o);
    }

  }

  private static String getNodeLabel(IR ir, BasicBlock bb) {
    StringBuffer result = new StringBuffer();

    int start = bb.getFirstInstructionIndex();
    int end = bb.getLastInstructionIndex();
    result.append("BB").append(bb.getNumber());
    if (bb.isEntryBlock()) {
      result.append(" (en)\\n");
    } else if (bb.isExitBlock()) {
      result.append(" (ex)\\n");
    }
    if (bb instanceof ExceptionHandlerBasicBlock) {
      result.append("<Handler>");
    }
    result.append("\\n");
    for (Iterator it = bb.iteratePhis(); it.hasNext();) {
      SSAPhiInstruction phi = (SSAPhiInstruction) it.next();
      if (phi != null) {
        result.append("           " + phi.toString()).append("\\n");
      }
    }
    if (bb instanceof ExceptionHandlerBasicBlock) {
      ExceptionHandlerBasicBlock ebb = (ExceptionHandlerBasicBlock) bb;
      SSAGetCaughtExceptionInstruction s = ebb.getCatchInstruction();
      if (s != null) {
        result.append("           " + s.toString()).append("\\n");
      } else {
        result.append("           " + " No catch instruction. Unreachable?\\n");
      }
    }
    SSAInstruction[] instructions = ir.getInstructions();
    for (int j = start; j <= end; j++) {
      if (instructions[j] != null) {
        StringBuffer x = new StringBuffer(j + "   " + instructions[j].toString());
        StringStuff.padWithSpaces(x, 35);
        result.append(x);
        result.append("\\n");
      }
    }
    for (Iterator it = bb.iteratePis(); it.hasNext();) {
      SSAPiInstruction pi = (SSAPiInstruction) it.next();
      if (pi != null) {
        result.append("           " + pi.toString()).append("\\n");
      }
    }
    return result.toString();
  }
}
