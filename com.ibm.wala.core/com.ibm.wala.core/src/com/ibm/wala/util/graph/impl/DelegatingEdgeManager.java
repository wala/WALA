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

import java.util.Iterator;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.INodeWithEdges;

/**
 *
 * An object that delegates edge management to the nodes, INodeWithEdges
 * 
 * @author sfink
 */
public class DelegatingEdgeManager implements EdgeManager {


  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator getPredNodes(Object N) {
    return ((INodeWithEdges)N).getPredNodes();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getPredNodeCount(Object N) {
    return ((INodeWithEdges)N).getPredNodeCount();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator getSuccNodes(Object N) {
    return ((INodeWithEdges)N).getSuccNodes();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getSuccNodeCount(Object N) {
    return ((INodeWithEdges)N).getSuccNodeCount();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(com.ibm.wala.util.graph.Node, com.ibm.wala.util.graph.Node)
   */
  public void addEdge(Object src, Object dst) {
    Assertions.UNREACHABLE();
  }
  
  public void removeEdge(Object src, Object dst) {
    Assertions.UNREACHABLE();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeAllIncidentEdges(Object node) {
    INodeWithEdges n = (INodeWithEdges)node;
    n.removeAllIncidentEdges();
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeIncomingEdges(Object node) {
    INodeWithEdges n = (INodeWithEdges)node;
    n.removeIncomingEdges();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeOutgoingEdges(Object node) {
    INodeWithEdges n = (INodeWithEdges)node;
    n.removeOutgoingEdges();
  }

  /* (non-Javadoc)
   */
  public boolean hasEdge(Object src, Object dst) {
    Assertions.UNREACHABLE("Implement me");
    return false;
  }
  
}
