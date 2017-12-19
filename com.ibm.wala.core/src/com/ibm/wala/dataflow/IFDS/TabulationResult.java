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
package com.ibm.wala.dataflow.IFDS;

import java.util.Collection;

import com.ibm.wala.util.intset.IntSet;

/**
 * The solution of a tabulation problem: a mapping from supergraph node -&gt; bit vector representing the dataflow facts that hold at
 * the entry to the supergraph node.
 * 
 * @param <T> type of node in the supergraph
 * @param <P> type of a procedure, like a box in an RSM
 * @param <F> type of factoids propagated when solving this problem
 */
public interface TabulationResult<T, P, F> {
  /**
   * get the bitvector of facts that hold at IN for a given node in the supergraph.
   * 
   * @param node a node in the supergraph
   * @return SparseIntSet efficiently representing the bitvector
   */
  public IntSet getResult(T node);

  /**
   * @return the governing IFDS problem
   */
  public TabulationProblem<T, P, F> getProblem();

  /**
   * @return the set of supergraph nodes for which any fact is reached
   */
  public Collection<T> getSupergraphNodesReached();

  /**
   * @param n1
   * @param d1
   * @param n2
   * @return set of d2 s.t. (n1,d1) -&gt; (n2,d2) is recorded as a summary edge, or null if none found
   */
  public IntSet getSummaryTargets(T n1, int d1, T n2);

  /**
   * @return the set of all {@link PathEdge}s that were used as seeds during the tabulation.
   */
  public Collection<PathEdge<T>> getSeeds();

}
