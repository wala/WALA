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

/**
 * Basic interface for a directed graph.
 */
public interface Graph<T> extends NodeManager<T>, EdgeManager<T> {
  /**
   * remove a node and all its incident edges
   * @throws UnsupportedOperationException if the graph implementation does not allow removal
   */
  public void removeNodeAndEdges(T n) throws UnsupportedOperationException;

}
