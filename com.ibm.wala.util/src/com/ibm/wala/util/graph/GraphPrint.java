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

import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

/**
 * Simple graph printing utility
 */
public class GraphPrint {

  public static <T> String genericToString(Graph<T> G) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    SlowSparseNumberedGraph<T> sg = SlowSparseNumberedGraph.make();
    for (T name : G) {
      sg.addNode(name);
    }
    for (T n : G) {
      for (T d : Iterator2Iterable.make(G.getSuccNodes(n))) {
        sg.addEdge(n,d);
      }
    }
    return sg.toString();
  }

}
