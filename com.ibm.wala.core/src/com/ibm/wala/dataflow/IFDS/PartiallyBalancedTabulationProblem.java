/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.dataflow.IFDS;

/**
 * A {@link TabulationProblem} with additional support for computing with partially balanced
 * parentheses.
 *
 * @param <T> type of node in the supergraph
 * @param <P> type of a procedure (like a box in an RSM)
 * @param <F> type of factoids propagated when solving this problem
 */
public interface PartiallyBalancedTabulationProblem<T, P, F> extends TabulationProblem<T, P, F> {

  @Override
  public IPartiallyBalancedFlowFunctions<T> getFunctionMap();

  /**
   * If n is reached by a partially balanced parenthesis, what is the entry node we should use as
   * the root of the {@link PathEdge} to n? Note that the result <em>must</em> in fact be an entry
   * node of the procedure containing n.
   */
  public T getFakeEntry(T n);
}
