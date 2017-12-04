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
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

public class SSACFGNullPointerAnalysis implements ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> {

  private final TypeReference[] ignoredExceptions;
  private IntraprocNullPointerAnalysis<ISSABasicBlock> intra = null;
  private final IR ir;
  private final ParameterState initialState;
  private final MethodState mState;
  
  public SSACFGNullPointerAnalysis(TypeReference[] ignoredExceptions, IR ir, ParameterState paramState, MethodState mState) {
    this.ignoredExceptions = (ignoredExceptions != null ? ignoredExceptions.clone() : null);
    this.ir = ir;
    this.initialState = (paramState == null ? ParameterState.createDefault(ir.getMethod()) : paramState);
    this.mState = (mState == null ? MethodState.DEFAULT : mState);
  }
  
  /*
   * @see com.ibm.wala.cfg.exc.ExceptionPruningAnalysis#compute(com.ibm.wala.util.MonitorUtil.IProgressMonitor)
   */
  @Override
  public int compute(IProgressMonitor progress) throws UnsoundGraphException, CancelException {
    ControlFlowGraph<SSAInstruction, ISSABasicBlock> orig = ir.getControlFlowGraph();

    intra = new IntraprocNullPointerAnalysis<>(ir, orig, ignoredExceptions, initialState, mState);
    intra.run(progress);

    return intra.getNumberOfDeletedEdges();
  }

  /* (non-Javadoc)
   * @see jsdg.exceptions.ExceptionPrunedCFGAnalysis#getPruned()
   */
  @Override
  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG() {
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
    
    ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = intra.getPrunedCFG();
    
    boolean hasException = false;
    for (ISSABasicBlock bb : cfg) {
      if (bb.getLastInstruction() == null) continue;
      List<ISSABasicBlock> succ = cfg.getExceptionalSuccessors(bb);
      if (succ != null && !succ.isEmpty()) {
        hasException = true;
        break;
      }
    }
    
    return hasException;
  }

  @Override
  public NullPointerState getState(ISSABasicBlock bb) {
    if (intra == null) {
      throw new IllegalStateException("Run compute(IProgressMonitor) first.");
    }
    
    return intra.getState(bb);
  }
  
}
