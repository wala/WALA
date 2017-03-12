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

import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeManager;

/**
 * A graph view that reverses the edges in a graph
 */
public class InvertedGraph<T> extends AbstractGraph<T> {

  final private NodeManager<T> nodes;

  @Override
  protected NodeManager<T> getNodeManager() {
    return nodes;
  }

  final private EdgeManager<T> edges;

  @Override
  protected EdgeManager<T> getEdgeManager() {
    return edges;
  }

  public InvertedGraph(Graph<T> G) {
    nodes = G;
    edges = new InvertingEdgeManager<>(G);
  }

}
