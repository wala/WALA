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
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ExplodedCFGNullPointerAnalysis implements ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> {
  
  private final TypeReference[] ignoredExceptions;
  private IntraprocNullPointerAnalysis<IExplodedBasicBlock> intra;
  private final IR ir;
  private final ParameterState initialState;
  private final MethodState mState;

  public ExplodedCFGNullPointerAnalysis(TypeReference[] ignoredExceptions, IR ir, ParameterState paramState, MethodState mState) {
    this.ignoredExceptions = (ignoredExceptions != null ? ignoredExceptions.clone() : null);
    this.ir = ir;
    this.initialState = (paramState == null ? ParameterState.createDefault(ir.getMethod()) : paramState);
    this.mState = (mState == null ? MethodState.DEFAULT : mState);
  }
  
  /* (non-Javadoc)
   * @see jsdg.exceptions.ExceptionPrunedCFGAnalysis#getOriginal()
   */
  public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getOriginal() {
    return ExplodedControlFlowGraph.make(ir);
  }
  /*
   * @see com.ibm.wala.cfg.exc.ExceptionPrunedCFGAnalysis#compute(com.ibm.wala.util.MonitorUtil.IProgressMonitor)
   */
  public int compute(IProgressMonitor progress) throws UnsoundGraphException, CancelException {
    ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> orig = getOriginal();

    intra = new IntraprocNullPointerAnalysis<IExplodedBasicBlock>(ir, orig, ignoredExceptions, initialState, mState);
    intra.run(progress);
    
    return intra.getNumberOfDeletedEdges();
  }
  
  /* (non-Javadoc)
   * @see jsdg.exceptions.ExceptionPrunedCFGAnalysis#getPruned()
   */
  public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getPruned() {
    if (intra == null) {
      throw new IllegalStateException("Run compute(IProgressMonitor) first.");
    }
    
    return intra.getPrunedCfg();
  }

  /* (non-Javadoc)
   * @see edu.kit.ipd.wala.ExceptionPrunedCFGAnalysis#hasExceptions()
   */
  public boolean hasExceptions() {
    if (intra == null) {
      throw new IllegalStateException("Run compute(IProgressMonitor) first.");
    }
    
    ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg = intra.getPrunedCfg();
    
    boolean hasException = false;
    for (IExplodedBasicBlock bb : cfg) {
      if (bb.getInstruction() == null) continue;
      List<IExplodedBasicBlock> succ = cfg.getExceptionalSuccessors(bb);
      if (succ != null && !succ.isEmpty()) {
        hasException = true;
        break;
      }
    }
    
    return hasException;
  }

  public NullPointerState getState(IExplodedBasicBlock bb) {
    if (intra == null) {
      throw new IllegalStateException("Run compute(IProgressMonitor) first.");
    }
    
    return intra.getState(bb);
  }
}
