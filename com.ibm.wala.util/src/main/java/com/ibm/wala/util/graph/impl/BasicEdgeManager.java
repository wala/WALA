/*
 * Copyright (c) 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manu Sridharan - initial API and implementation
 */
package com.ibm.wala.util.graph.impl;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.graph.EdgeManager;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Simple implementation of an {@link com.ibm.wala.util.graph.EdgeManager}. Does not support edge
 * deletion.
 */
public class BasicEdgeManager<T> implements EdgeManager<T> {
  private final Map<T, Set<T>> preds = HashMapFactory.make();

  private final Map<T, Set<T>> succs = HashMapFactory.make();

  @Override
  public Iterator<T> getPredNodes(T n) {
    Set<T> nodePreds = this.preds.get(n);
    return nodePreds != null ? nodePreds.iterator() : Collections.emptyIterator();
  }

  @Override
  public int getPredNodeCount(T n) {
    Set<T> nodePreds = this.preds.get(n);
    return nodePreds != null ? nodePreds.size() : 0;
  }

  @Override
  public Iterator<T> getSuccNodes(T n) {
    Set<T> nodeSuccs = this.succs.get(n);
    return nodeSuccs != null ? nodeSuccs.iterator() : Collections.emptyIterator();
  }

  @Override
  public int getSuccNodeCount(T n) {
    Set<T> nodeSuccs = this.succs.get(n);
    return nodeSuccs != null ? nodeSuccs.size() : 0;
  }

  @Override
  public void addEdge(T src, T dst) {
    MapUtil.findOrCreateSet(succs, src).add(dst);
    MapUtil.findOrCreateSet(preds, dst).add(src);
  }

  @Override
  public void removeEdge(T src, T dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAllIncidentEdges(T node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeIncomingEdges(T node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeOutgoingEdges(T node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasEdge(T src, T dst) {
    Set<T> succsForSrc = succs.get(src);
    return succsForSrc != null && succsForSrc.contains(dst);
  }
}
