/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
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
import java.util.Map;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.SimpleVector;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.OrderedMultiGraph;

/**
 * Inefficient implementation of OrderedMultiGraph.
 * 
 * UNDER CONSTRUCTION.
 * 
 * @param <T> type of node in the graph
 */
public class BasicOrderedMultiGraph<T> implements OrderedMultiGraph<T> {

  final Map<T, SimpleVector<T>> successorEdges = HashMapFactory.make();

  private final Graph<T> delegate;

  public BasicOrderedMultiGraph() {
    this.delegate = SlowSparseNumberedGraph.make();
  }

  /**
   * Add this edge, unconditionally setting it as the next successor.
   */
  public void addEdge(T src, T dst) throws IllegalArgumentException {
    delegate.addEdge(src, dst);
    SimpleVector<T> s = successorEdges.get(src);
    if (s == null) {
      s = new SimpleVector<T>();
      successorEdges.put(src, s);
    }
    s.set(s.getMaxIndex() + 1, dst);
  }

  public void addEdge(int i, T src, T dst) throws IllegalArgumentException {
    delegate.addEdge(src, dst);
    SimpleVector<T> s = successorEdges.get(src);
    if (s == null) {
      s = new SimpleVector<T>();
      successorEdges.put(src, s);
    }
    s.set(i, dst);
  }

  public void addNode(T n) {
    delegate.addNode(n);
  }

  public boolean containsNode(T N) {
    return delegate.containsNode(N);
  }

  public int getNumberOfNodes() {
    return delegate.getNumberOfNodes();
  }

  public int getPredNodeCount(T N) throws IllegalArgumentException {
    return delegate.getPredNodeCount(N);
  }

  /**
   * For now, this returns nodes in no particular order! Fix this when needed.
   */
  public Iterator<T> getPredNodes(T N) throws IllegalArgumentException {
    return delegate.getPredNodes(N);
  }

  public int getSuccNodeCount(T N) throws IllegalArgumentException {
    return delegate.getSuccNodeCount(N);
  }

  public Iterator<T> getSuccNodes(T N) throws IllegalArgumentException {
    return delegate.getSuccNodes(N);
  }

  public boolean hasEdge(T src, T dst) {
    return delegate.hasEdge(src, dst);
  }

  public Iterator<T> iterator() {
    return delegate.iterator();
  }

  public void removeAllIncidentEdges(T node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    delegate.removeAllIncidentEdges(node);
  }

  public void removeEdge(T src, T dst) throws UnimplementedError {
    Assertions.UNREACHABLE();
    delegate.removeEdge(src, dst);
  }

  public void removeIncomingEdges(T node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    delegate.removeIncomingEdges(node);
  }

  public void removeNode(T n) throws UnimplementedError {
    Assertions.UNREACHABLE();
    delegate.removeNode(n);
  }

  public void removeNodeAndEdges(T N) throws UnimplementedError {
    Assertions.UNREACHABLE();
    delegate.removeNodeAndEdges(N);
  }

  public void removeOutgoingEdges(T node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    delegate.removeOutgoingEdges(node);
  }

  public T getSuccessor(T node, int i) throws IllegalArgumentException {
    SimpleVector<T> s = successorEdges.get(node);
    if (s == null) {
      throw new IllegalArgumentException("no successors for node " + node);
    }
    if (i > s.getMaxIndex()) {
      throw new IllegalArgumentException("no successor number " + i + " for " + node);
    }
    return s.get(i);
  }

}
