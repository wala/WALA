/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;



/**
 * A {@link TabulationSolver} that gives up after a finite bound.
 * 
 * @param <T> type of node in the supergraph 
 * @param <P> type of a procedure (like a box in an RSM)
 * @param <F> type of factoids propagated when solving this problem
 *
 */
public class BoundedPartiallyBalancedSolver<T, P, F> extends PartiallyBalancedTabulationSolver<T, P, F> {

  private final static boolean VERBOSE = false;
  
  public static <T, P, F> BoundedPartiallyBalancedSolver<T, P, F> createdBoundedPartiallyBalancedSolver(PartiallyBalancedTabulationProblem<T, P, F> p, int bound,
      IProgressMonitor monitor) {
    return new BoundedPartiallyBalancedSolver<>(p, bound, monitor);
  }

  private final int bound;
  
  private int numSteps = 0;
  
  protected BoundedPartiallyBalancedSolver(PartiallyBalancedTabulationProblem<T, P, F> p, int bound, IProgressMonitor monitor) {
    super(p, monitor);
    this.bound = bound;
  }
  
  @Override
  protected boolean propagate(T s_p, int i,T n, int j) {
    if (numSteps < bound) {
      numSteps++;
      return super.propagate(s_p, i, n, j);
    } else {
      if (VERBOSE) {
        System.err.println("Suppressing propagation; reached bound " + s_p + " " + i + " " + n + " " + j);
      }
      return false;
    }
  }
  
  public int getNumSteps() {
    return numSteps;
  }
  
  public void resetBound() {
    numSteps = 0;
  }

}
