package com.ibm.wala.dataflow.graph;

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
import com.ibm.wala.fixpoint.BooleanVariable;
import com.ibm.wala.fixpoint.IVariable;

/**
 * @author sfink
 */
public class BooleanSolver<T> extends DataflowSolver<T> {

  /**
   * @param problem
   */
  public BooleanSolver(IKilldallFramework<T> problem) {
    super(problem);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.graph.IterativeSolver#makeVariable(java.lang.Object,
   *      boolean)
   */
  protected IVariable makeNodeVariable(Object n, boolean IN) {

    return new BooleanVariable(n.hashCode() * 97381 + (IN ? 0 : 1));

  }

  protected IVariable makeEdgeVariable(Object src, Object dst) {

    return new BooleanVariable(src.hashCode() ^ dst.hashCode());

  }

}
