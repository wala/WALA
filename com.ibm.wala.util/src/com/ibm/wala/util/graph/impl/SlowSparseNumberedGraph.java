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
package com.ibm.wala.util.graph.impl;

import java.io.Serializable;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.BasicNaturalRelation;

/**
 * A graph of numbered nodes, expected to have a fairly sparse edge structure.
 */
public class SlowSparseNumberedGraph<T> extends AbstractNumberedGraph<T> implements Serializable {

  private static final long serialVersionUID = 7014361126159594838L;

  private final SlowNumberedNodeManager<T> nodeManager = new SlowNumberedNodeManager<>();

  private final SparseNumberedEdgeManager<T> edgeManager;

  protected SlowSparseNumberedGraph() {
    this(0);
  }

  /**
   * If normalOutCount == n, this edge manager will eagerly allocated n words to hold out edges for each node. (performance
   * optimization for time)
   * 
   * @param normalOutCount what is the "normal" number of out edges for a node?
   */
  public SlowSparseNumberedGraph(int normalOutCount) {
    edgeManager = new SparseNumberedEdgeManager<>(nodeManager, normalOutCount, BasicNaturalRelation.TWO_LEVEL);
  }

  /*
   * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
   */
  @Override
  public NumberedNodeManager<T> getNodeManager() {
    return nodeManager;
  }

  /*
   * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
   */
  @Override
  public NumberedEdgeManager<T> getEdgeManager() {
    return edgeManager;
  }

  /**
   * @return a graph with the same nodes and edges as g
   */
  public static <T> SlowSparseNumberedGraph<T> duplicate(Graph<T> g) {
    SlowSparseNumberedGraph<T> result = make();
    copyInto(g, result);
    return result;
  }

  public static <T> void copyInto(Graph<T> g, Graph<T> into) {
    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    for (T name : g) {
      into.addNode(name);
    }
    for (T n : g) {
      for (T succ : Iterator2Iterable.make(g.getSuccNodes(n))) {
        into.addEdge(n, succ);
      }
    }
  }

  public static <T> SlowSparseNumberedGraph<T> make() {
    return new SlowSparseNumberedGraph<>();
  }
}
