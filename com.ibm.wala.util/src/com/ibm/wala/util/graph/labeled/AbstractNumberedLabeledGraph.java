/*******************************************************************************
 * Copyright (c) 2007 Juergen Graf
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Juergen Graf
 *******************************************************************************/
package com.ibm.wala.util.graph.labeled;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.intset.IntSet;

public abstract class AbstractNumberedLabeledGraph<T, U> extends AbstractNumberedGraph<T> implements LabeledGraph<T, U> {

  /**
   * @return the object which manages edges in the graph
   */
  @Override
  protected abstract NumberedLabeledEdgeManager<T, U> getEdgeManager();

  public void addEdge(T src, T dst, U label) {
    getEdgeManager().addEdge(src, dst, label);
  }

  public Iterator<? extends U> getPredLabels(T N) {
    return getEdgeManager().getPredLabels(N);
  }

  public int getPredNodeCount(T N, U label) {
    return getEdgeManager().getPredNodeCount(N, label);
  }

  public Iterator<T> getPredNodes(T N, U label) {
    return getEdgeManager().getPredNodes(N, label);
  }

  public Iterator<? extends U> getSuccLabels(T N) {
    return getEdgeManager().getSuccLabels(N);
  }

  public int getSuccNodeCount(T N, U label) {
    return getEdgeManager().getSuccNodeCount(N, label);
  }

  public Iterator<? extends T> getSuccNodes(T N, U label) {
    return getEdgeManager().getSuccNodes(N, label);
  }

  public IntSet getPredNodeNumbers(T node, U label) throws IllegalArgumentException {
    return getEdgeManager().getPredNodeNumbers(node, label);
  }

  public IntSet getSuccNodeNumbers(T node, U label) throws IllegalArgumentException {
    return getEdgeManager().getSuccNodeNumbers(node, label);
  }
  
  public boolean hasEdge(T src, T dst, U label) {
    return getEdgeManager().hasEdge(src, dst, label);
  }

  public void removeEdge(T src, T dst, U label) {
    getEdgeManager().removeEdge(src, dst, label);
  }

  public Set<? extends U> getEdgeLabels(T src, T dst) {
    return getEdgeManager().getEdgeLabels(src, dst);
  }

  public U getDefaultLabel() {
    return getEdgeManager().getDefaultLabel();
  }
}
