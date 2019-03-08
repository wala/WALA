/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.dataflow.IFDS;

import java.util.Collection;

/**
 * Representation of a Dyck-language graph reachability problem for the tabulation solver.
 *
 * <p>Special case: if supportsMerge(), then the problem is not really IFDS anymore. (TODO: rename
 * it?). Instead, we perform a merge operation before propagating at every program point. This way,
 * we can implement standard interprocedural dataflow and ESP-style property simulation, and various
 * other things.
 *
 * <p>Note that at the moment, the data structures in the TabulationSolver are not set up to do
 * merge efficiently. TODO.
 *
 * <p>See Reps, Horwitz, Sagiv POPL 95
 *
 * @param <T> type of node in the supergraph
 * @param <P> type of a procedure (like a box in an RSM)
 * @param <F> type of factoids propagated when solving this problem
 */
public interface TabulationProblem<T, P, F> {

  public ISupergraph<T, P> getSupergraph();

  public TabulationDomain<F, T> getDomain();

  public IFlowFunctionMap<T> getFunctionMap();

  /** Define the set of path edges to start propagation with. */
  public Collection<PathEdge<T>> initialSeeds();

  /**
   * Special case: if supportsMerge(), then the problem is not really IFDS anymore. (TODO: rename
   * it?). Instead, we perform a merge operation before propagating at every program point. This
   * way, we can implement standard interprocedural dataflow and ESP-style property simulation, and
   * various other things.
   *
   * @return the merge function, or null if !supportsMerge()
   */
  public IMergeFunction getMergeFunction();
}
