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
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.Map;
import java.util.function.Predicate;

/** Exploded interprocedural control-flow graph, constructed lazily. */
public class ExplodedInterproceduralCFG extends AbstractInterproceduralCFG<IExplodedBasicBlock> {

  /** Caching to improve runtime .. hope it doesn't turn into a memory leak. */
  private Map<CGNode, ExplodedControlFlowGraph> cfgMap;

  public static ExplodedInterproceduralCFG make(CallGraph cg) {
    return new ExplodedInterproceduralCFG(cg);
  }

  protected ExplodedInterproceduralCFG(CallGraph cg) {
    super(cg);
  }

  public ExplodedInterproceduralCFG(CallGraph cg, Predicate<CGNode> filter) {
    super(cg, filter);
  }

  /**
   * @return the cfg for n, or null if none found
   * @throws IllegalArgumentException if n == null
   */
  @Override
  public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getCFG(CGNode n)
      throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("n == null");
    }
    if (cfgMap == null) {
      // we have to initialize this lazily since this might be called from a super() constructor
      cfgMap = HashMapFactory.make();
    }
    ExplodedControlFlowGraph result = cfgMap.get(n);
    if (result == null) {
      IR ir = n.getIR();
      if (ir == null) {
        return null;
      }
      result = ExplodedControlFlowGraph.make(ir);
      cfgMap.put(n, result);
    }
    return result;
  }
}
