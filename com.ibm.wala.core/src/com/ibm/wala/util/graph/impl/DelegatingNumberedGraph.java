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
import com.ibm.wala.util.graph.NodeManager;


/**
 * @author sfink
 */
public class DelegatingNumberedGraph extends AbstractNumberedGraph {

  private DelegatingNumberedNodeManager nodeManager = new DelegatingNumberedNodeManager();
  private DelegatingNumberedEdgeManager edgeManager = new DelegatingNumberedEdgeManager(nodeManager);

  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
   */
  protected NodeManager getNodeManager() {
    return nodeManager;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
   */
  protected EdgeManager getEdgeManager() {
    return edgeManager;
  }
}