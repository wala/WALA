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

import com.ibm.wala.util.graph.NumberedGraph;

/**
 * Calculate dominators using Langauer and Tarjan's fastest algorithm. TOPLAS
 * 1(1), July 1979. This implementation uses path compression and results in a
 * O(e * alpha(e,n)) complexity, where e is the number of edges in the CFG and n
 * is the number of nodes.
 * 
 * Sources: TOPLAS article, Muchnick book
 */

public class NumberedDominators<T> extends Dominators<T> {

  public NumberedDominators(NumberedGraph<T> G, T root) throws IllegalArgumentException {
    super(G, root);

    this.infoMap = new Object[G.getMaxNumber() + 1];
    for (T n : G) {
      infoMap[G.getNumber(n)] = new DominatorInfo(n);
    }

    analyze();
  }

  /*
   * Look-aside table for DominatorInfo objects
   */
  private final Object[] infoMap;

  @SuppressWarnings("unchecked")
  @Override
  protected final DominatorInfo getInfo(T node) {
    assert node != null;
    return (DominatorInfo) infoMap[((NumberedGraph<T>)G).getNumber(node)];
  }

}
