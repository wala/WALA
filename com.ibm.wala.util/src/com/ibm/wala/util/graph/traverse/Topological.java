/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.graph.traverse;

import java.util.Iterator;

import com.ibm.wala.util.collections.ReverseIterator;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.GraphInverter;

/**
 * Utilities for iterating over graphs in topological order.
 */
public class Topological {

  /**
   * Build an Iterator over all the nodes in the graph, in an order such that SCCs are visited in topological order.
   * 
   * @throws IllegalArgumentException if graph == null
   */
  public static <T> Iterable<T> makeTopologicalIter(final Graph<T> graph) throws IllegalArgumentException {
    if (graph == null) {
      throw new IllegalArgumentException("graph == null");
    }

    return () -> {
      // the following code ensures a topological order over SCCs.
      // note that the first two lines of the following give a topological
      // order for dags, but that can get screwed up by cycles. so
      // instead, we use Tarjan's SCC algorithm, which happens to
      // visit nodes in an order consistent with a top. order over SCCs.

      // finish time is post-order
      // note that if you pay attention only to the first representative
      // of each SCC discovered, we have a top. order of these SCC
      // representatives
      Iterator<T> finishTime = DFS.iterateFinishTime(graph);
      // reverse postorder is usual topological sort.
      Iterator<T> rev = ReverseIterator.reverse(finishTime);
      // the following statement helps out the GC; note that finishTime holds
      // on to a large array
      finishTime = null;
      Graph<T> G_T = GraphInverter.invert(graph);
      return DFS.iterateFinishTime(G_T, rev);
    };
  }
}
