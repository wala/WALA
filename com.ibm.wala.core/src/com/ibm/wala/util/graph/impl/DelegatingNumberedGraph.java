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
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.INodeWithNumberedEdges;
import com.ibm.wala.util.graph.NodeManager;

/**
 * @author sfink
 */
public class DelegatingNumberedGraph<T extends INodeWithNumberedEdges> extends AbstractNumberedGraph<T> {

  private DelegatingNumberedNodeManager<T> nodeManager = new DelegatingNumberedNodeManager<T>();

  private DelegatingNumberedEdgeManager<T> edgeManager = new DelegatingNumberedEdgeManager<T>(nodeManager);

  /*
   * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
   */
  @Override
  protected NodeManager<T> getNodeManager() {
    return nodeManager;
  }

  /*
   * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
   */
  @Override
  protected EdgeManager<T> getEdgeManager() {
    return edgeManager;
  }
}