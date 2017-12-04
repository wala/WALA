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
package com.ibm.wala.util.graph.dominators;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * An object that computes the dominance frontiers of a graph
 */
public class DominanceFrontiers<T> {

  final private Map<T, Set<T>> DF = HashMapFactory.make();

  final private Dominators<T> dom;

  final private Graph<T> G;

  final private T root;

  /**
   * @param G
   *          The graph
   * @param root
   *          The root from which to compute dominators
   */
  public DominanceFrontiers(Graph<T> G, T root) {
    this.root = root;
    this.G = G;
    this.dom = Dominators.make(G, root);
    analyze();
  }

  public Iterator<T> getDominanceFrontier(T n) {
    Set<T> frontier = DF.get(n);
    if (frontier == null) {
      throw new IllegalArgumentException("no dominance frontier for node " + n);
    }
    return frontier.iterator();
  }

  public boolean isDominatedBy(T node, T master) {
    return dom.isDominatedBy(node, master);
  }

  public Iterator<T> dominators(T node) {
    return dom.dominators(node);
  }

  public Graph<T> dominatorTree() {
    return dom.dominatorTree();
  }

  private void analyze() {
    Graph<T> DT = dom.dominatorTree();

    Iterator<T> XS = DFS.iterateFinishTime(DT, new NonNullSingletonIterator<>(root));
    while (XS.hasNext()) {
      T X = XS.next();
      Set<T> DF_X = HashSetFactory.make();
      DF.put(X, DF_X);

      // DF_local
      for (T Y : Iterator2Iterable.make(G.getSuccNodes(X))) {
        if (dom.getIdom(Y) != X) {
          DF_X.add(Y);
        }
      }

      // DF_up
      for (T Z : Iterator2Iterable.make(DT.getSuccNodes(X))) {
        for (T Y2 : Iterator2Iterable.make(getDominanceFrontier(Z))) {
          if (dom.getIdom(Y2) != X)
            DF_X.add(Y2);
        }
      }
    }
  }
}
