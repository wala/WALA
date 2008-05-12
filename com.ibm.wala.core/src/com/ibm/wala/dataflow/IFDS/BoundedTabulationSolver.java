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

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.wala.eclipse.util.CancelException;

/**
 * A {@link TabulationSolver} that gives up after a finite bound.
 * 
 * @author sjfink
 *
 */
public class BoundedTabulationSolver<T, P> extends TabulationSolver<T, P> {

  public static <T, P> BoundedTabulationSolver<T, P> createBoundedTabulationSolver(TabulationProblem<T, P> p, int bound,
      IProgressMonitor monitor) {
    return new BoundedTabulationSolver<T, P>(p, bound, monitor);
  }

  private final int bound;
  
  private int numSteps = 0;
  
  protected BoundedTabulationSolver(TabulationProblem<T, P> p, int bound, IProgressMonitor monitor) {
    super(p, monitor);
    this.bound = bound;
  }
  
  @Override
  protected void propagate(T s_p, int i,T n, int j) {
    if (numSteps < bound) {
      numSteps++;
      super.propagate(s_p, i, n, j);
    }
  }
  
  public int getNumSteps() {
    return numSteps;
  }

  @Override
  public TabulationResult<T, P> solve() throws CancelException {
    numSteps = 0;
    return super.solve();
  }

  @Override
  public void addSeed(PathEdge<T> seed) {
    numSteps = 0;
    super.addSeed(seed);
  }
}
