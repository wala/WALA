/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cfg.exc.intra;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntPair;

/**
 * Nullpointer analysis - NOT A REAL KILDALL framework instance, because the transfer
 * functions are not distribute (similar to constant propagation). Therefore we remove 
 * back edges in the flow graph.
 * 
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 *
 */
public class NullPointerFrameWork<T extends ISSABasicBlock> implements IKilldallFramework<T, NullPointerState> {

  private final Graph<T> flow;
  private final NullPointerTransferFunctionProvider<T> transferFunct;
  
  public NullPointerFrameWork(ControlFlowGraph<SSAInstruction, T> cfg, IR ir) {
    final IBinaryNaturalRelation backEdges = Acyclic.computeBackEdges(cfg, cfg.entry());
    boolean hasBackEdge = backEdges.iterator().hasNext();
    if (hasBackEdge) {
      MutableCFG<SSAInstruction, T> cfg2 = MutableCFG.copyFrom(cfg);
      
      for (IntPair edge : backEdges) {
        T from = cfg2.getNode(edge.getX());
        T to = cfg2.getNode(edge.getY());
        cfg2.removeEdge(from, to);
        cfg2.addEdge(from, cfg.exit());
      }
      
      this.flow = cfg2;
    } else {
      this.flow = cfg;
    }

    this.transferFunct = new NullPointerTransferFunctionProvider<>(cfg, ir);
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.graph.IKilldallFramework#getFlowGraph()
   */
  @Override
  public Graph<T> getFlowGraph() {
    return flow;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.graph.IKilldallFramework#getTransferFunctionProvider()
   */
  @Override
  public NullPointerTransferFunctionProvider<T> getTransferFunctionProvider() {
    return transferFunct;
  }
  
}
