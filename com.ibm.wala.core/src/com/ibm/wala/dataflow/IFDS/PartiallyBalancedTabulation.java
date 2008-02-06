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
import java.util.Iterator;

import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * Utilities for dealing with tabulation with partially balanced parentheses.
 * 
 * @author sjfink
 * 
 */
public class PartiallyBalancedTabulation {

  /**
   * Compute a set of <n, d1> pairs that represent return statement factoids that should be considered reached in a
   * partially balanced parentheses tabulation.
   * 
   * We assume that the given {@link TabulationResult} represents a solution to a fully balanced tabulation. We look at
   * any exit statements that were reached, and compute the corresponding return factoids that could be reached, if we
   * ignored parentheses. We return the set of such factoids, which represent roots to restart tabulation with.
   */
  public static <T, P> Collection<Pair<T, Integer>> computeNewSeeds(TabulationResult<T, P> tabulation, PartiallyBalancedTabulationProblem<T, P> problem) {
    Collection<Pair<T, Integer>> result = HashSetFactory.make();

    ISupergraph<T, P> supergraph = tabulation.getProblem().getSupergraph();
    // for each path edge used as a seed ...
    for (PathEdge<T> entry : tabulation.getSeeds()) {
      // for each exit from the local procedure of the seed _entry_ ..
      for (T exit : supergraph.getExitsForProcedure(supergraph.getProcOf(entry.getEntry()))) {
        IntSet reached = tabulation.getSummaryTargets(entry.getEntry(), entry.getD1(), exit);
        if (reached != null) {
          // for each summary edge between entry.getEntry() and exit ...
          for (IntIterator it = reached.intIterator(); it.hasNext();) {
            int d2 = it.next();
            // d2 was reached from an entry seed. if there are any facts which are reachable from d2, even without
            // balanced parentheses, we can use these as new seeds.
            for (Iterator<? extends T> it2 = supergraph.getSuccNodes(exit); it2.hasNext();) {
              T retSite = it2.next();
              IFlowFunction f = problem.getReturnFlowFunction(exit, retSite);
              // for each fact that can be reached by the return flow ...
              if (f instanceof IUnaryFlowFunction) {
                IUnaryFlowFunction uf = (IUnaryFlowFunction) f;
                IntSet facts = uf.getTargets(d2);
                for (IntIterator it4 = facts.intIterator(); it4.hasNext();) {
                  int d3 = it4.next();
                  // d3 would be reached of we ignored parentheses. use it as a new seed.
                  result.add(Pair.make(retSite, d3));
                }
              } else {
                Assertions.UNREACHABLE("Partially balanced logic not supported for binary return flow functions");
              }
            }
          }
        }
      }
    }
    return result;
  }
  
  /**
   * Solve a partially balanced tabulation problem.
   * 
   * @param <T> type of node in the supergraph
   * @param <P> type of "procedure" ("box") in the supergraph
   * @param problem representation of the dataflow problem
   */
  public static <T, P> TabulationResult<T, P> tabulate(PartiallyBalancedTabulationProblem<T, P> problem) throws CancelException {
    TabulationSolver<T, P> solver = TabulationSolver.make(problem);
    return tabulate(problem, solver);
  }

  /**
   * Solve a partially balanced tabulation problem using a pre-constructed solver.
   * 
   * @param <T> type of node in the supergraph
   * @param <P> type of "procedure" ("box") in the supergraph
   * @param problem representation of the dataflow problem
   */
  public static <T, P> TabulationResult<T, P> tabulate(PartiallyBalancedTabulationProblem<T, P> problem, TabulationSolver<T, P> solver) throws CancelException {
    TabulationResult<T, P> tr = null;

    boolean again = true;
    while (again) {
      tr = solver.solve();

      Collection<Pair<T, Integer>> newRoots = computeNewSeeds(tr, problem);
      Collection<PathEdge<T>> newSeeds = toPathEdges(newRoots, problem);

      newSeeds.removeAll(tr.getSeeds());
      again = !newSeeds.isEmpty();
      for (PathEdge<T> seed : newSeeds) {
        solver.addSeed(seed);
      }
    }
    return tr;
  }

  /**
   * Convert a set of reachable factoids to {@link PathEdge}s, using the appropriate fake entry nodes as
   * determined by the {@link PartiallyBalancedTabulationProblem}.
   */
  private static <T, P> Collection<PathEdge<T>> toPathEdges(Collection<Pair<T, Integer>> newRoots,
      PartiallyBalancedTabulationProblem<T, P> problem) {
    Collection<PathEdge<T>> result = HashSetFactory.make();
    for (Pair<T, Integer> p : newRoots) {
      T fakeEntry = problem.getFakeEntry(p.fst);
      result.add(PathEdge.createPathEdge(fakeEntry, p.snd, p.fst, p.snd));
    }
    return result;
  }

}
