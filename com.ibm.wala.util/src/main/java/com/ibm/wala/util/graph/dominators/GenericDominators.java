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
package com.ibm.wala.util.graph.dominators;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.Graph;
import java.util.Map;

/**
 * Calculate dominators using Langauer and Tarjan's fastest algorithm. TOPLAS 1(1), July 1979. This
 * implementation uses path compression and results in a O(e * alpha(e,n)) complexity, where e is
 * the number of edges in the CFG and n is the number of nodes.
 *
 * <p>Sources: TOPLAS article, Muchnick book
 */
public class GenericDominators<T> extends Dominators<T> {

  public GenericDominators(Graph<T> G, T root) throws IllegalArgumentException {
    super(G, root);
    this.infoMap = HashMapFactory.make(G.getNumberOfNodes());
    analyze();
  }

  /*
   * Look-aside table for DominatorInfo objects
   */
  private final Map<Object, DominatorInfo> infoMap;

  @Override
  protected DominatorInfo getInfo(T node) {
    if (!infoMap.containsKey(node)) infoMap.put(node, new DominatorInfo(node));
    return infoMap.get(node);
  }
}
