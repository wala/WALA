/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.graph;

import com.ibm.wala.util.collections.Iterator2Iterable;
import java.util.Iterator;
import java.util.stream.Stream;

/** Basic functionality for a {@link Graph} that delegates node and edge management. */
public abstract class AbstractGraph<T> implements Graph<T> {

  /** @return the object which manages nodes in the graph */
  protected abstract NodeManager<T> getNodeManager();

  /** @return the object which manages edges in the graph */
  protected abstract EdgeManager<T> getEdgeManager();

  @Override
  public Stream<T> stream() {
    return getNodeManager().stream();
  }

  /** @see Graph#iterator() */
  @Override
  public Iterator<T> iterator() {
    return getNodeManager().iterator();
  }

  /** @see com.ibm.wala.util.graph.Graph#getNumberOfNodes() */
  @Override
  public int getNumberOfNodes() {
    return getNodeManager().getNumberOfNodes();
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object) */
  @Override
  public int getPredNodeCount(T n) throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("n cannot be null");
    }
    return getEdgeManager().getPredNodeCount(n);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object) */
  @Override
  public Iterator<T> getPredNodes(T n) throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("n cannot be null");
    }
    return getEdgeManager().getPredNodes(n);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object) */
  @Override
  public int getSuccNodeCount(T n) throws IllegalArgumentException {
    if (!containsNode(n) || n == null) {
      throw new IllegalArgumentException("node not in graph " + n);
    }
    return getEdgeManager().getSuccNodeCount(n);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object) */
  @Override
  public Iterator<T> getSuccNodes(T n) throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("n cannot be null");
    }
    return getEdgeManager().getSuccNodes(n);
  }

  /** @see com.ibm.wala.util.graph.NodeManager#addNode(Object) */
  @Override
  public void addNode(T n) {
    getNodeManager().addNode(n);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#addEdge(Object, Object) */
  @Override
  public void addEdge(T src, T dst) throws IllegalArgumentException {
    getEdgeManager().addEdge(src, dst);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#removeEdge(java.lang.Object, java.lang.Object) */
  @Override
  public void removeEdge(T src, T dst) throws IllegalArgumentException {
    getEdgeManager().removeEdge(src, dst);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#hasEdge(java.lang.Object, java.lang.Object) */
  @Override
  public boolean hasEdge(T src, T dst) {
    if (src == null) {
      throw new IllegalArgumentException("src is null");
    }
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    return getEdgeManager().hasEdge(src, dst);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(Object) */
  @Override
  public void removeAllIncidentEdges(T node) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    getEdgeManager().removeAllIncidentEdges(node);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(Object) */
  @Override
  public void removeIncomingEdges(T node) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    getEdgeManager().removeIncomingEdges(node);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(Object) */
  @Override
  public void removeOutgoingEdges(T node) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    getEdgeManager().removeOutgoingEdges(node);
  }

  /** @see com.ibm.wala.util.graph.Graph#removeNode(Object) */
  @Override
  public void removeNodeAndEdges(T N) throws IllegalArgumentException {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    getEdgeManager().removeAllIncidentEdges(N);
    getNodeManager().removeNode(N);
  }

  /** @see com.ibm.wala.util.graph.NodeManager#removeNode(Object) */
  @Override
  public void removeNode(T n) throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    getNodeManager().removeNode(n);
  }

  protected String nodeString(T n, @SuppressWarnings("unused") boolean forEdge) {
    return n.toString();
  }

  protected String edgeString(
      @SuppressWarnings("unused") T from, @SuppressWarnings("unused") T to) {
    return " --> ";
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (T n : this) {
      sb.append(nodeString(n, false)).append('\n');
      for (T s : Iterator2Iterable.make(getSuccNodes(n))) {
        sb.append(edgeString(n, s)).append(nodeString(s, true));
        sb.append('\n');
      }
      sb.append('\n');
    }

    return sb.toString();
  }

  /** @see com.ibm.wala.util.graph.NodeManager#containsNode(Object) */
  @Override
  public boolean containsNode(T n) {
    if (n == null) {
      throw new IllegalArgumentException("n cannot be null");
    }
    return getNodeManager().containsNode(n);
  }
}
