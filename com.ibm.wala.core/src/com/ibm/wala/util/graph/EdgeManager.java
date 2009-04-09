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

import org.w3c.dom.Node;

/**
 * An object which manages edges in a directed graph.
 */
public interface EdgeManager<T> {

  /**
   * Return an Iterator over the immediate predecessor {@link Node nodes} of this Node in the Graph.
   * 
   * This method never returns <code>null</code>.
   * 
   * @return an Iterator over the immediate predecessor nodes of this Node.
   */
  public Iterator<? extends T> getPredNodes(T N);

  /**
   * Return the number of {@link #getPredNodes immediate predecessor} {@link Node nodes} of this Node in the Graph.
   * 
   * @return the number of immediate predecessor Nodes of this Node in the Graph.
   */
  public int getPredNodeCount(T N);

  /**
   * Return an Iterator over the immediate successor {@link Node nodes} of this Node in the Graph
   * <p>
   * This method never returns <code>null</code>.
   * 
   * @return an Iterator over the immediate successor Nodes of this Node.
   */
  public Iterator<? extends T> getSuccNodes(T N);

  /**
   * Return the number of {@link #getSuccNodes immediate successor} {@link Node nodes} of this Node in the Graph
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
