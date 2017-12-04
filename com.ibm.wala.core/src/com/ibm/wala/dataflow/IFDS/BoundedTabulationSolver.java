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
public class BoundedTabulationSolver<T, P, F> extends TabulationSolver<T, P, F> {

  public static <T, P, F> BoundedTabulationSolver<T, P, F> createBoundedTabulationSolver(TabulationProblem<T, P, F> p, int bound,
      IProgressMonitor monitor) {
    return new BoundedTabulationSolver<>(p, bound, monitor);
  }

  private final int bound;
  
  private int numSteps = 0;
  
  protected BoundedTabulationSolver(TabulationProblem<T, P, F> p, int bound, IProgressMonitor monitor) {
    super(p, monitor);
    this.bound = bound;
  }
  
  @Override
  protected boolean propagate(T s_p, int i,T n, int j) {
    if (numSteps < bound) {
      numSteps++;
      return super.propagate(s_p, i, n, j);
    }
    return false;
  }
  
  public int getNumSteps() {
    return numSteps;
  }
  
  public void resetBound() {
    numSteps = 0;
  }

}
