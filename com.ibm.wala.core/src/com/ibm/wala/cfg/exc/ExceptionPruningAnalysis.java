package com.ibm.wala.cfg.exc;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.exc.intra.NullPointerState;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

/**
 * This abstract class is used as interface for analysis that remove impossible
 * control flow from a CFG. This is done by detecting exceptions that may always
 * (or never) appear.
 * 
 * @author Juergen Graf <graf@kit.edu>
 * 
 */
public interface ExceptionPruningAnalysis<I, T extends IBasicBlock<I>> {

  /**
   * Computes impossible control flow that is due to exceptions that definitely
   * will not appear or that will always be thrown. You have to run this method
   * before using getPruned() to extract the result of the analysis.
   * @param progress
   *          A progress monitor that is used to display the progress of the
   *          analysis. It can also be used to detect a cancel request from the
   *          user. The common behavior is to cancel the method if
   *          progress.isCanceled() is true by throwing a CancelException.
   * @return Number of edges that have been removed from the cfg.
   * @throws UnsoundGraphException
   *           Thrown if the original CFG contains inconsistencies.
   * @throws CancelException
   *           Thrown if the user requested cancellation through the progress
   *           monitor.
  */
  int compute(IProgressMonitor progress) throws UnsoundGraphException, CancelException;
  
  /**
   * Returns the result of the analysis: A control flow graph where impossible
   * control flow has been removed. The way how and which impossible flow is
   * detected may vary between different implementations of this class.
   * Run compute(IProgressMonitor) first.
   * 
   * @return The improved CFG without edges that were detected as impossible
   *         flow.
   */
  ControlFlowGraph<I, T> getPruned();

  /**
   * Returns true if the corresponding method contains instructions that may
   * throw an exception. Run compute(IPrograssMonitor) first.
   * @return true if the corresponding method contains instructions that may
   * throw an exception.
   */
  boolean hasExceptions();

  /**
   * Returns the control flow graph that is used as starting point of this
   * analysis. This should be the original CFG without any deleted edges.
   * 
   * @return The original CFG of the given method.
   * @throws UnsoundGraphException
   *           Thrown if the original CFG contains inconsistencies.
   */
  ControlFlowGraph<I, T> getOriginal() throws UnsoundGraphException;
  
  /**
   * Returns the state of a node. The node has to be part of the cfg.
   * @param bb Node
   * @return EdgeState
   */
  NullPointerState getState(T bb);

}
