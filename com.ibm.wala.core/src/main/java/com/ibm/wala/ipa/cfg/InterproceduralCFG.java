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
package com.ibm.wala.ipa.cfg;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import java.util.function.Predicate;

/**
 * Interprocedural control-flow graph.
 *
 * <p>TODO: think about a better implementation; perhaps a lazy view of the constituent CFGs Lots of
 * ways this can be optimized?
 */
public class InterproceduralCFG extends AbstractInterproceduralCFG<ISSABasicBlock> {

  public InterproceduralCFG(CallGraph CG) {
    super(CG);
  }

  public InterproceduralCFG(CallGraph cg, Predicate<CGNode> filtersection) {
    super(cg, filtersection);
  }

  /**
   * @return the cfg for n, or null if none found
   * @throws IllegalArgumentException if n == null
   */
  @Override
  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode n)
      throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("n == null");
    }
    if (n.getIR() == null) {
      return null;
    }
    ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = n.getIR().getControlFlowGraph();
    if (cfg == null) {
      return null;
    }

    return cfg;
  }
}
