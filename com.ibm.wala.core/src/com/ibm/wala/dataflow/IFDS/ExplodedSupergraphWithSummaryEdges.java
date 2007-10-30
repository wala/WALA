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

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * This version of the exploded supergraph includes summary edges as deduced by
 * the tabulation solver
 * 
 * @author sfink
 * 
 */
public class ExplodedSupergraphWithSummaryEdges<T> extends ExplodedSupergraph<T> {

  private final TabulationSolver<T, ?> solver;

  /**
   * @param supergraph
   * @param flowFunctions
   * @param solver
   */
  public ExplodedSupergraphWithSummaryEdges(ISupergraph<T, ?> supergraph, IFlowFunctionMap<T> flowFunctions,
      TabulationSolver<T, ?> solver) {
    super(supergraph, flowFunctions);
    this.solver = solver;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ExplodedSupergraph#getSuccNodes(java.lang.Object)
   */
  @Override
  public Iterator<ExplodedSupergraphNode<T>> getSuccNodes(ExplodedSupergraphNode<T> src) {
    if (src == null) {
      throw new IllegalArgumentException("src is null");
    }
    HashSet<ExplodedSupergraphNode<T>> result = HashSetFactory.make(5);
    result.addAll(Iterator2Collection.toCollection(super.getSuccNodes(src)));

    // add facts from summary edges
    if (getSupergraph().isCall(src.getSupergraphNode())) {
      for (Iterator<? extends T> it = getSupergraph().getReturnSites(src.getSupergraphNode()); it.hasNext();) {
        T dest = it.next();
        Assertions.UNREACHABLE();
        IntSet summary = null;
//        IntSet summary = solver.getSummaryTargets(src.getSupergraphNode(), src.getFact(), dest);
        if (summary != null) {
          for (IntIterator ii = summary.intIterator(); ii.hasNext();) {
            int d2 = ii.next();
            result.add(new ExplodedSupergraphNode<T>(dest, d2));
          }
        }
      }
    }
    return result.iterator();
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ExplodedSupergraph#getPredNodes(java.lang.Object)
   */
  @Override
  public Iterator<ExplodedSupergraphNode<T>> getPredNodes(ExplodedSupergraphNode<T> dest) {
    if (dest == null) {
      throw new IllegalArgumentException("dest is null");
    }
    HashSet<ExplodedSupergraphNode<T>> result = HashSetFactory.make(5);
    result.addAll(Iterator2Collection.toCollection(super.getPredNodes(dest)));

    // add facts from summary edges
    if (getSupergraph().isReturn(dest.getSupergraphNode())) {
      for (Iterator<? extends T> it = getSupergraph().getCallSites(dest.getSupergraphNode()); it.hasNext();) {
        T src = it.next();
        IntSet summary = solver.getSummarySources(dest.getSupergraphNode(), dest.getFact(), src);
        if (summary != null) {
          for (IntIterator ii = summary.intIterator(); ii.hasNext();) {
            int d1 = ii.next();
            result.add(new ExplodedSupergraphNode<T>(src, d1));
          }
        }
      }
    }
    return result.iterator();
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ExplodedSupergraph#getPredNodeCount(java.lang.Object)
   */
  @Override
  public int getPredNodeCount(ExplodedSupergraphNode<T> N) {
    return Iterator2Collection.toCollection(getPredNodes(N)).size();
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ExplodedSupergraph#getSuccNodeCount(java.lang.Object)
   */
  @Override
  public int getSuccNodeCount(ExplodedSupergraphNode<T> N) {
    return Iterator2Collection.toCollection(getSuccNodes(N)).size();
  }
}
