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

import com.ibm.wala.util.graph.Graph;

/**
 * A utility class.
 */
public class DelegatingGraph<T> implements Graph<T> {

  private final Graph<T> delegate;

  public DelegatingGraph(Graph<T> delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("delegate is null");
    }
    this.delegate = delegate;
  }

  public void addEdge(T src, T dst) throws IllegalArgumentException {
    delegate.addEdge(src, dst);
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

  @Override
  public String toString() {
    return delegate.toString();
  }

  public int getPredNodeCount(T N) throws IllegalArgumentException {
    return delegate.getPredNodeCount(N);
  }

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

  public void removeAllIncidentEdges(T node) throws IllegalArgumentException {
    delegate.removeAllIncidentEdges(node);
  }

  public void removeEdge(T src, T dst) throws IllegalArgumentException {
    delegate.removeEdge(src, dst);
  }

  public void removeIncomingEdges(T node) throws IllegalArgumentException {
    delegate.removeIncomingEdges(node);
  }

  public void removeNode(T n) {
    delegate.removeNode(n);
  }

  public void removeNodeAndEdges(T N) throws IllegalArgumentException {
    delegate.removeNodeAndEdges(N);
  }

  public void removeOutgoingEdges(T node) throws IllegalArgumentException {
    delegate.removeOutgoingEdges(node);
  }

}
