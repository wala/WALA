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

/**
 * A {@link TabulationSolver} that gives up after a finite bound.
 * 
 * @author sjfink
 *
 */
public class BoundedPartiallyBalancedSolver<T, P> extends PartiallyBalancedTabulationSolver<T, P> {

  public static <T, P> BoundedPartiallyBalancedSolver<T, P> createBoundedParitallyBalancedSolver(PartiallyBalancedTabulationProblem<T, P> p, int bound,
      IProgressMonitor monitor) {
    return new BoundedPartiallyBalancedSolver<T, P>(p, bound, monitor);
  }

  private final int bound;
  
  private int numSteps = 0;
  
  protected BoundedPartiallyBalancedSolver(PartiallyBalancedTabulationProblem<T, P> p, int bound, IProgressMonitor monitor) {
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
  
  public void resetBound() {
    numSteps = 0;
  }

}
