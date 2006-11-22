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
package com.ibm.wala.dataflow.graph;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.IVariable;

/**
 * @author sfink
 */
public class BitVectorSolver<T> extends DataflowSolver<T> {

  /**
   * @param problem
   */
  public BitVectorSolver(IKilldallFramework<T> problem) {
    super(problem);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.graph.IterativeSolver#makeVariable(java.lang.Object,
   *      boolean)
   */
  protected IVariable makeNodeVariable(T n, boolean IN) {
    return new BitVectorVariable();
  }

  protected IVariable makeEdgeVariable(T src, T dst) {
    return new BitVectorVariable();

  }

}
