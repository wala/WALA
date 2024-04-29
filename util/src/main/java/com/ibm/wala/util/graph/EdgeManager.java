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
import org.jspecify.annotations.Nullable;

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
  Iterator<T> getPredNodes(@Nullable T n);

  /**
   * Return the number of {@link #getPredNodes immediate predecessor} nodes of n
   *
   * @return the number of immediate predecessors of n.
   */
  int getPredNodeCount(T n);

  /**
   * Return an Iterator over the immediate successor nodes of n
   *
   * <p>This method never returns {@code null}.
   *
   * @return an Iterator over the immediate successor nodes of n
   */
  Iterator<T> getSuccNodes(@Nullable T n);

  /**
   * Return the number of {@link #getSuccNodes immediate successor} nodes of this Node in the Graph
   *
   * @return the number of immediate successor Nodes of this Node in the Graph.
   */
  int getSuccNodeCount(T N);

  void addEdge(T src, T dst);

  void removeEdge(T src, T dst) throws UnsupportedOperationException;

  void removeAllIncidentEdges(T node) throws UnsupportedOperationException;

  void removeIncomingEdges(T node) throws UnsupportedOperationException;

  void removeOutgoingEdges(T node) throws UnsupportedOperationException;

  boolean hasEdge(@Nullable T src, @Nullable T dst);
}
