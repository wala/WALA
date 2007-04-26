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

  private final TabulationSolver<T,?> solver;

  /**
   * @param supergraph
   * @param flowFunctions
   * @param solver
   */
  public ExplodedSupergraphWithSummaryEdges(ISupergraph<T,?> supergraph, IFlowFunctionMap<T> flowFunctions, TabulationSolver<T,?> solver) {
    super(supergraph, flowFunctions);
    this.solver = solver;
  }
  

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.IFDS.ExplodedSupergraph#getSuccNodes(java.lang.Object)
   */
  public Iterator<ExplodedSupergraphNode<T>> getSuccNodes(ExplodedSupergraphNode<T> src) {
    HashSet<ExplodedSupergraphNode<T>> result = HashSetFactory.make(5);
    result.addAll(new Iterator2Collection<ExplodedSupergraphNode<T>>(super.getSuccNodes(src)));

    // add facts from summary edges
    if (getSupergraph().isCall(src.getSupergraphNode())) {
      for (Iterator<? extends T> it = getSupergraph().getReturnSites(src.getSupergraphNode()); it.hasNext();) {
        T dest = it.next();
        IntSet summary = solver.getSummaryTargets(src.getSupergraphNode(), src.getFact(), dest);
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



  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.IFDS.ExplodedSupergraph#getPredNodes(java.lang.Object)
   */
  public Iterator<ExplodedSupergraphNode<T>> getPredNodes(ExplodedSupergraphNode<T> dest) {
    HashSet<ExplodedSupergraphNode<T>> result = HashSetFactory.make(5);
    result.addAll(new Iterator2Collection<ExplodedSupergraphNode<T>>(super.getPredNodes(dest)));

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


  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.IFDS.ExplodedSupergraph#getPredNodeCount(java.lang.Object)
   */
  public int getPredNodeCount(ExplodedSupergraphNode<T> N) {
    return new Iterator2Collection<ExplodedSupergraphNode<T>>(getPredNodes(N)).size();
  }





  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.IFDS.ExplodedSupergraph#getSuccNodeCount(java.lang.Object)
   */
  public int getSuccNodeCount(ExplodedSupergraphNode<T> N) {
    return new Iterator2Collection<ExplodedSupergraphNode<T>>(getSuccNodes(N)).size();
  }

}
