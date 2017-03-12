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
import com.ibm.wala.util.graph.INodeWithNumberedEdges;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;

/**
 * Basic functionality for a graph that delegates node and edge management, and
 * tracks node numbers
 */
public class DelegatingNumberedGraph<T extends INodeWithNumberedEdges> extends AbstractNumberedGraph<T> {

  final private DelegatingNumberedNodeManager<T> nodeManager = new DelegatingNumberedNodeManager<>();

  final private DelegatingNumberedEdgeManager<T> edgeManager = new DelegatingNumberedEdgeManager<>(nodeManager);

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
