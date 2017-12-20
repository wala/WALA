/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
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
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * Utilities for dealing with tabulation with partially balanced parentheses.
 *
 * @param <T> type of node in the supergraph
 * @param <P> type of a procedure (like a box in an RSM)
 * @param <F> type of factoids propagated when solving this problem
 */
public class PartiallyBalancedTabulationSolver<T, P, F> extends TabulationSolver<T, P, F> {

  public static <T, P, F> PartiallyBalancedTabulationSolver<T, P, F> createPartiallyBalancedTabulationSolver(
      PartiallyBalancedTabulationProblem<T, P, F> p, IProgressMonitor monitor) {
    return new PartiallyBalancedTabulationSolver<>(p, monitor);
  }

  private final Collection<Pair<T,Integer>> unbalancedSeeds = HashSetFactory.make();

  protected PartiallyBalancedTabulationSolver(PartiallyBalancedTabulationProblem<T, P, F> p, IProgressMonitor monitor) {
    super(p, monitor);
  }

  @Override
  protected boolean propagate(T s_p, int i, T n, int j) {
    boolean result = super.propagate(s_p, i, n, j);
    if (result && wasUsedAsUnbalancedSeed(s_p, i) && supergraph.isExit(n)) {
      // j was reached from an entry seed. if there are any facts which are reachable from j, even without
      // balanced parentheses, we can use these as new seeds.
      for (T retSite : Iterator2Iterable.make(supergraph.getSuccNodes(n))) {
        PartiallyBalancedTabulationProblem<T, P, F> problem = (PartiallyBalancedTabulationProblem<T, P, F>) getProblem();
        IFlowFunction f = problem.getFunctionMap().getUnbalancedReturnFlowFunction(n, retSite);
        // for each fact that can be reached by the return flow ...
        if (f instanceof IUnaryFlowFunction) {
          IUnaryFlowFunction uf = (IUnaryFlowFunction) f;
          IntSet facts = uf.getTargets(j);
          if (facts != null) {
            for (IntIterator it4 = facts.intIterator(); it4.hasNext();) {
              int d3 = it4.next();
              // d3 would be reached if we ignored parentheses. use it as a new seed.
              T fakeEntry = problem.getFakeEntry(retSite);
              PathEdge<T> seed = PathEdge.createPathEdge(fakeEntry, d3, retSite, d3);
              addSeed(seed);
              newUnbalancedExplodedReturnEdge(s_p,  i, n, j);
            }
          }
        } else {
          Assertions.UNREACHABLE("Partially balanced logic not supported for binary return flow functions");
        }
      }
    }
    return result;
  }

  @Override
  public void addSeed(PathEdge<T> seed) {
    if (getSeeds().contains(seed)) {
      return;
    }
    unbalancedSeeds.add(Pair.make(seed.entry, seed.d1));
    super.addSeed(seed);

  }

  /**
   * Was the fact number i named at node s_p introduced as an "unbalanced" seed during partial tabulation?
   * If so, any facts "reached" from here can be further propagated with unbalanced parens.
   */
  private boolean wasUsedAsUnbalancedSeed(T s_p, int i) {
   return unbalancedSeeds.contains(Pair.make(s_p, i));
  }

  /**
   * A path edge &lt;s_p, i&gt; -&gt; &lt;n, j&gt; was propagated, and &lt;s_p, i&gt; was an unbalanced seed.
   * So, we added a new seed callerSeed (to some return site) in the caller.  To be overridden
   * in subclasses.
   */
  @SuppressWarnings("unused")
  protected void newUnbalancedExplodedReturnEdge(T s_p, int i, T n, int j) {

  }
}
