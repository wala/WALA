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

import java.util.List;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

/**
 * Intraprocedural null pointer analysis for the exploded control flow graph.
 * 
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 *
 */
public class ExplodedCFGNullPointerAnalysis implements ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> {
  
  private final TypeReference[] ignoredExceptions;
  private IntraprocNullPointerAnalysis<IExplodedBasicBlock> intra;
  private final IR ir;
  private final ParameterState initialState;
  private final MethodState mState;
  private final boolean optHasExceptions;

  public ExplodedCFGNullPointerAnalysis(TypeReference[] ignoredExceptions, IR ir, ParameterState paramState, MethodState mState, boolean optHasExceptions) {
    this.ignoredExceptions = (ignoredExceptions != null ? ignoredExceptions.clone() : null);
    this.ir = ir;
    this.initialState = (paramState == null ? ParameterState.createDefault(ir.getMethod()) : paramState);
    this.mState = (mState == null ? MethodState.DEFAULT : mState);
    this.optHasExceptions = optHasExceptions;
  }

  /*
   * @see com.ibm.wala.cfg.exc.ExceptionPrunedCFGAnalysis#compute(com.ibm.wala.util.MonitorUtil.IProgressMonitor)
   */
  @Override
  public int compute(IProgressMonitor progress) throws UnsoundGraphException, CancelException {
    ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> orig = ExplodedControlFlowGraph.make(ir);

    intra = new IntraprocNullPointerAnalysis<>(ir, orig, ignoredExceptions, initialState, mState);
    intra.run(progress);
    
    return intra.getNumberOfDeletedEdges();
  }
  
  /* (non-Javadoc)
   * @see jsdg.exceptions.ExceptionPrunedCFGAnalysis#getCFG()
   */
  @Override
  public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getCFG() {
    if (intra == null) {
      throw new IllegalStateException("Run compute(IProgressMonitor) first.");
    }
    
    return intra.getPrunedCFG();
  }

  /* (non-Javadoc)
   * @see edu.kit.ipd.wala.ExceptionPrunedCFGAnalysis#hasExceptions()
   */
  @Override
  public boolean hasExceptions() {
    if (intra == null) {
      throw new IllegalStateException("Run compute(IProgressMonitor) first.");
    }
    
    ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg = intra.getPrunedCFG();
    
    boolean hasException = false;
    for (IExplodedBasicBlock bb : cfg) {
      if (bb.getInstruction() == null) continue;
      List<IExplodedBasicBlock> succ = cfg.getExceptionalSuccessors(bb);
      if (succ != null && !succ.isEmpty() && (!optHasExceptions || succ.contains(cfg.exit()))) {
        hasException = true;
        break;
      }
    }
    
    return hasException;
  }

  @Override
  public NullPointerState getState(IExplodedBasicBlock bb) {
    if (intra == null) {
      throw new IllegalStateException("Run compute(IProgressMonitor) first.");
    }
    
    return intra.getState(bb);
  }
}
