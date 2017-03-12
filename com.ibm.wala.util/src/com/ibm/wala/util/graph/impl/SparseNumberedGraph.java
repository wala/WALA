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

import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.BasicNaturalRelation;

/**
 * A graph of numbered nodes, expected to have a fairly sparse edge structure.
 */
public class SparseNumberedGraph<T extends INodeWithNumber> extends AbstractNumberedGraph<T> {

  private final DelegatingNumberedNodeManager<T> nodeManager;

  private final SparseNumberedEdgeManager<T> edgeManager;

  public SparseNumberedGraph() {
    nodeManager = new DelegatingNumberedNodeManager<>();
    edgeManager = new SparseNumberedEdgeManager<>(nodeManager);
  }

  /**
   * If normalCase == n, the s edge manager will eagerly allocated n words to hold out edges for each node. (performance
   * optimization for time)
   * 
   * @param normalCase what is the "normal" number of out edges for a node?
   */
  public SparseNumberedGraph(int normalCase) {
    nodeManager = new DelegatingNumberedNodeManager<>();
    edgeManager = new SparseNumberedEdgeManager<>(nodeManager, normalCase, BasicNaturalRelation.TWO_LEVEL);
  }

  public SparseNumberedGraph(DelegatingNumberedNodeManager<T> nodeManager, SparseNumberedEdgeManager<T> edgeManager) {
    this.nodeManager = nodeManager;
    this.edgeManager = edgeManager;
  }

  /*
   * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
   */
  @Override
  protected NumberedNodeManager<T> getNodeManager() {
    return nodeManager;
  }

  /*
   * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
   */
  @Override
  protected NumberedEdgeManager<T> getEdgeManager() {
    return edgeManager;
  }
}
