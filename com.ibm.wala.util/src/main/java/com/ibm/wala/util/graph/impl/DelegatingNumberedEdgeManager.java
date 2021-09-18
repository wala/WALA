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
package com.ibm.wala.util.graph.impl;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.INodeWithNumberedEdges;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;
import java.util.Iterator;

/** An object that delegates edge management to the nodes, {@link INodeWithNumberedEdges} */
public class DelegatingNumberedEdgeManager<T extends INodeWithNumberedEdges>
    implements NumberedEdgeManager<T> {

  private final DelegatingNumberedNodeManager<T> nodeManager;

  public DelegatingNumberedEdgeManager(DelegatingNumberedNodeManager<T> nodeManager) {
    if (nodeManager == null) {
      throw new IllegalArgumentException("nodeManager is null");
    }
    this.nodeManager = nodeManager;
  }

  // TODO: optimization is possible
  private class IntSetNodeIterator implements Iterator<T> {

    private final IntIterator delegate;

    IntSetNodeIterator(IntIterator delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
      return delegate.hasNext();
    }

    @Override
    public T next() {
      return nodeManager.getNode(delegate.next());
    }

    @Override
    public void remove() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
    }
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(Object) */
  @Override
  public Iterator<T> getPredNodes(T N) throws IllegalArgumentException {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    INodeWithNumberedEdges en = N;
    IntSet pred = en.getPredNumbers();
    Iterator<T> empty = EmptyIterator.instance();
    return (pred == null) ? empty : (Iterator<T>) new IntSetNodeIterator(pred.intIterator());
  }

  @Override
  public IntSet getPredNodeNumbers(T node) {
    if (node == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    INodeWithNumberedEdges en = node;
    IntSet pred = en.getPredNumbers();
    return (pred == null) ? new SparseIntSet() : pred;
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(Object) */
  @Override
  public int getPredNodeCount(T N) throws IllegalArgumentException {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    INodeWithNumberedEdges en = N;
    IntSet s = en.getPredNumbers();
    if (s == null) {
      return 0;
    } else {
      return s.size();
    }
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(Object) */
  @Override
  public Iterator<T> getSuccNodes(T N) {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    INodeWithNumberedEdges en = N;
    IntSet succ = en.getSuccNumbers();
    Iterator<T> empty = EmptyIterator.instance();
    return (succ == null) ? empty : (Iterator<T>) new IntSetNodeIterator(succ.intIterator());
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(Object) */
  @Override
  public int getSuccNodeCount(T N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    INodeWithNumberedEdges en = N;
    IntSet s = en.getSuccNumbers();
    return s == null ? 0 : s.size();
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#addEdge(Object, Object) */
  @Override
  public void addEdge(T src, T dst) {
    if (dst == null || src == null) {
      throw new IllegalArgumentException("parameter is null");
    }
    src.addSucc(dst.getGraphNodeId());
    dst.addPred(src.getGraphNodeId());
  }

  @Override
  public void removeEdge(T src, T dst) throws UnimplementedError {
    Assertions.UNREACHABLE("Implement me");
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(Object) */
  @Override
  public void removeAllIncidentEdges(T node) throws UnimplementedError {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    INodeWithNumberedEdges n = node;
    n.removeAllIncidentEdges();
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(Object) */
  @Override
  public void removeIncomingEdges(T node) throws UnimplementedError {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    INodeWithNumberedEdges n = node;
    n.removeIncomingEdges();
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(Object) */
  @Override
  public void removeOutgoingEdges(T node) throws UnimplementedError {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    INodeWithNumberedEdges n = node;
    n.removeOutgoingEdges();
  }

  @Override
  public boolean hasEdge(T src, T dst) throws IllegalArgumentException {
    if (dst == null) {
      throw new IllegalArgumentException("dst == null");
    }
    return getSuccNodeNumbers(src).contains(dst.getGraphNodeId());
  }

  @Override
  public IntSet getSuccNodeNumbers(T node) {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    INodeWithNumberedEdges en = node;
    IntSet succ = en.getSuccNumbers();
    return (succ == null) ? new SparseIntSet() : succ;
  }
}
