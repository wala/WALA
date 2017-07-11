/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.fixpoint;



import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

/**
 * Solves a set of constraints
 */
public interface IFixedPointSolver<T extends IVariable<T>>  {

  /**
   * @return the set of statements solved by this {@link IFixedPointSolver}
   */
  public IFixedPointSystem<T> getFixedPointSystem();
  
  /**
   * Solve the problem.
   * <p>
   * PRECONDITION: graph is set up
   * 
   * @return true iff the evaluation of some constraint caused a change in the
   *         value of some variable.
   */
  public boolean solve(IProgressMonitor monitor) throws CancelException;
}
