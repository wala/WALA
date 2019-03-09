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

import java.util.Iterator;

/**
 * An object which manages edges in a directed graph.
 *
 * @param <T> the type of node in the graph
 */
public interface EdgeManager<T> {

  /**
   * Return an {@link Iterator} over the immediate predecessor nodes of n
   *
   * <p>This method never returns {@code null}.
   *
   * @return an {@link Iterator} over the immediate predecessor nodes of this Node.
   */
  public Iterator<T> getPredNodes(T n);

  /**
   * Return the number of {@link #getPredNodes immediate predecessor} nodes of n
   *
   * @return the number of immediate predecessors of n.
   */
  public int getPredNodeCount(T n);

  /**
   * Return an Iterator over the immediate successor nodes of n
   *
   * <p>This method never returns {@code null}.
   *
   * @return an Iterator over the immediate successor nodes of n
   */
  public Iterator<T> getSuccNodes(T n);

  /**
   * Return the number of {@link #getSuccNodes immediate successor} nodes of this Node in the Graph
   *
   * @return the number of immediate successor Nodes of this Node in the Graph.
   */
  public int getSuccNodeCount(T N);

  public void addEdge(T src, T dst);

  public void removeEdge(T src, T dst) throws UnsupportedOperationException;

  public void removeAllIncidentEdges(T node) throws UnsupportedOperationException;

  public void removeIncomingEdges(T node) throws UnsupportedOperationException;

  public void removeOutgoingEdges(T node) throws UnsupportedOperationException;

  public boolean hasEdge(T src, T dst);
}
