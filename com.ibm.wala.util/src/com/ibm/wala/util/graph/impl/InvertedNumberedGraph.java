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
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.NumberedNodeManager;

/**
 * A graph view that reverses the edges in a graph
 */
public class InvertedNumberedGraph<T> extends AbstractNumberedGraph<T> {

  final private NumberedNodeManager<T> nodes;
  final private NumberedEdgeManager<T> edges;

  @Override
  protected NumberedNodeManager<T> getNodeManager() {
    return nodes;
  }

  @Override
  protected NumberedEdgeManager<T> getEdgeManager() {
    return edges;
  }

  InvertedNumberedGraph(NumberedGraph<T> G) {
    nodes = G;
    edges = new InvertingNumberedEdgeManager<>(G);
  }

}
