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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.util.debug.Trace;

/**
 * 
 * standard fixed-point iterative solver for pointer analysis
 * 
 * @author sfink
 */
public class StandardSolver extends AbstractPointsToSolver {

  /**
   * @param system
   * @param builder
   */
  public StandardSolver(PropagationSystem system, PropagationCallGraphBuilder builder) {
    super(system, builder);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.IPointsToSolver#solve()
   */
  @Override
  public void solve() throws IllegalArgumentException, CancelException {
    
    int i = 0;
    do {
      i++;

      if (DEBUG) {
        Trace.println("Iteration " + i);
      }
      getSystem().solve();
      if (DEBUG) {
        Trace.println("Solved " + i);
      }

      // Add constraints until there are no new discovered nodes
      getBuilder().addConstraintsFromNewNodes();

      getReflectionHandler().updateForReflection();
      // Note that we may have added stuff to the
      // worklist; so,
    } while (!getSystem().emptyWorkList());
    
  }

}
