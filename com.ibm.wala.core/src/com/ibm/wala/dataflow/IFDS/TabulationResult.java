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
 * The solution of a tabulation problem: a mapping from supergraph node ->
 * bit vector representing the dataflow facts that hold at the enty to the
 * supergraph node.
 * 
 * @author sfink
 */
public interface TabulationResult<T, P> {
  /**
   * get the bitvector of facts that hold at the entry to a given node
   * 
   * @param node
   * @return SparseIntSet efficiently representing the bitvector
   */
  public IntSet getResult(T node);
  
  /**
   * @return the governing IFDS problem
   */
  public TabulationProblem<T, P> getProblem();

  /**
   * @return the set of supergraph nodes for which any fact is reached
   */
  public Collection<T> getSupergraphNodesReached();

}
