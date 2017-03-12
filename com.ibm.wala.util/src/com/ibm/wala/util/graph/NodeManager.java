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

import java.util.Collection;
import java.util.Iterator;

/**
 * An object which tracks graph nodes.
 * 
 * This is effectively a stripped-down collection interface. We choose to avoid using the full {@link Collection} interface, so that
 * it takes less code to implement a new {@link NodeManager}.
 * 
 * @param <T> the type of nodes this {@link NodeManager} tracks.
 */
public interface NodeManager<T> extends Iterable<T> {

  /**
   * @return an {@link Iterator} of the nodes in this graph
   */
  @Override
  public Iterator<T> iterator();

  /**
   * @return the number of nodes in this graph
   */
  public int getNumberOfNodes();

  /**
   * add a node to this graph
   */
  public void addNode(T n);

  /**
   * remove a node from this graph
   */
  public void removeNode(T n) throws UnsupportedOperationException;

  /**
   * @return true iff the graph contains the specified node
   */
  public boolean containsNode(T n);
}
