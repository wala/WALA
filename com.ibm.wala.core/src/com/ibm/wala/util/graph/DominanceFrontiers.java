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
package com.ibm.wala.util.graph;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * 
 * An object that computes the dominance frontiers of a graph
 * 
 * @author Julian Dolby
 */
public class DominanceFrontiers<T> extends Dominators<T> {

  final private Map<T, Set<T>> DF = HashMapFactory.make();

  /**
   * @param G
   *          The graph
   * @param root
   *          The root from which to compute dominators
   */
  public DominanceFrontiers(Graph<T> G, T root) {
    super(G, root);
    analyze();
  }

  public Iterator<T> getDominanceFrontier(T n) {
    return DF.get(n).iterator();
  }

  private void analyze() {
    Graph<T> DT = dominatorTree();

    Iterator<T> XS = DFS.iterateFinishTime(DT, new NonNullSingletonIterator<T>(root));
    while (XS.hasNext()) {
      T X = XS.next();
      Set<T> DF_X = HashSetFactory.make();
      DF.put(X, DF_X);

      // DF_local
      for (Iterator<? extends T> YS = G.getSuccNodes(X); YS.hasNext();) {
        T Y = YS.next();
        if (getIdom(Y) != X) {
          DF_X.add(Y);
        }
      }

      // DF_up
      for (Iterator<? extends T> ZS = DT.getSuccNodes(X); ZS.hasNext();) {
        T Z = ZS.next();
        for (Iterator<T> YS2 = getDominanceFrontier(Z); YS2.hasNext();) {
          T Y2 = YS2.next();
          if (getIdom(Y2) != X)
            DF_X.add(Y2);
        }
      }
    }
  }
}
