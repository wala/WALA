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

/**
 * Basic functionality for a graph that delegates node and edge management.
 * 
 * @author sfink
 */
public abstract class AbstractGraph<T> implements Graph<T> {

  /**
   * @return the object which manages nodes in the graph
   */
  protected abstract NodeManager<T> getNodeManager();

  /**
   * @return the object which manages edges in the graph
   */
  protected abstract EdgeManager<T> getEdgeManager();

  /*
   * @see com.ibm.wala.util.graph.Graph#iterateNodes()
   */
  public Iterator<T> iterator() {
    return getNodeManager().iterator();
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#getNumberOfNodes()
   */
  public int getNumberOfNodes() {
    return getNodeManager().getNumberOfNodes();
  }

  public int getPredNodeCount(T N) throws IllegalArgumentException {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    return getEdgeManager().getPredNodeCount(N);
  }

  public Iterator<? extends T> getPredNodes(T N) throws IllegalArgumentException {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    return getEdgeManager().getPredNodes(N);
  }

  public int getSuccNodeCount(T N) throws IllegalArgumentException {
    if (!containsNode(N) || N == null) {
      throw new IllegalArgumentException("node not in graph " + N);
    }
    return getEdgeManager().getSuccNodeCount(N);
  }

  public Iterator<? extends T> getSuccNodes(T N) throws IllegalArgumentException {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    return getEdgeManager().getSuccNodes(N);
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#addNode(com.ibm.wala.util.graph.Node)
   */
  public void addNode(T n) {
    getNodeManager().addNode(n);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(com.ibm.wala.util.graph.Node,
   *      com.ibm.wala.util.graph.Node)
   */
  public void addEdge(T src, T dst) throws IllegalArgumentException {
    getEdgeManager().addEdge(src, dst);
  }

  public void removeEdge(T src, T dst) throws IllegalArgumentException {
    getEdgeManager().removeEdge(src, dst);
  }

  public boolean hasEdge(T src, T dst) {
    if (src == null) {
      throw new IllegalArgumentException("src is null");
    }
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    return getEdgeManager().hasEdge(src, dst);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeAllIncidentEdges(T node) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    getEdgeManager().removeAllIncidentEdges(node);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeIncomingEdges(T node) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    getEdgeManager().removeIncomingEdges(node);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeOutgoingEdges(T node) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    getEdgeManager().removeOutgoingEdges(node);
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#removeNode(com.ibm.wala.util.graph.Node)
   */
  public void removeNodeAndEdges(T N) throws IllegalArgumentException {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    getEdgeManager().removeAllIncidentEdges(N);
    getNodeManager().removeNode(N);
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#remove(com.ibm.wala.util.graph.Node)
   */
  public void removeNode(T n) throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    getNodeManager().removeNode(n);
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (Iterator<? extends T> ns = iterator(); ns.hasNext();) {
      T n = ns.next();
      sb.append(n.toString()).append("\n");
      for (Iterator ss = getSuccNodes(n); ss.hasNext();) {
        Object s = ss.next();
        sb.append("  --> ").append(s);
        sb.append("\n");
      }
      sb.append("\n");
    }

    return sb.toString();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(com.ibm.wala.util.graph.Node)
   */
  public boolean containsNode(T N) {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    return getNodeManager().containsNode(N);
  }

}
