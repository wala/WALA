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
 * @author Juergen Graf &lt;graf@kit.edu&gt;
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
  ControlFlowGraph<I, T> getCFG();

  /**
   * Returns true if the corresponding method contains instructions that may
   * throw an exception which is not caught in the same method.
   * Run compute(IPrograssMonitor) first.
   * @return true if the corresponding method contains instructions that may
   * throw an exception which is not caught in the same method
   */
  boolean hasExceptions();

  /**
   * Returns the state of a node. The node has to be part of the cfg.
   * @param bb Node
   * @return NullPointerState
   */
  NullPointerState getState(T bb);

}
