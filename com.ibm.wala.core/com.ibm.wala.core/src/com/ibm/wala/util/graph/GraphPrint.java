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

import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

/**
 *
 * Simple graph printing utility
 * 
 * @author sfink
 */
public class GraphPrint {

  public static <T> String genericToString(Graph<T> G) {
    SlowSparseNumberedGraph<T> sg = new SlowSparseNumberedGraph<T>();
    for (Iterator<? extends T> it = G.iterateNodes(); it.hasNext(); ) {
      sg.addNode(it.next());
    }
    for (Iterator<? extends T> it = G.iterateNodes(); it.hasNext(); ) {
      T n = it.next();
      for (Iterator<? extends T> it2 = G.getSuccNodes(n); it2.hasNext(); ) {
        T d = it2.next();
        sg.addEdge(n,d);
      }
    }
    return sg.toString();
  }

}
