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
 *
 *
 * @author Stephen Fink
 */
public interface Graph<T> extends NodeManager<T>, EdgeManager<T> {
  /**
   * remove a node and all its incident edges
   */
  public void removeNodeAndEdges(T N) throws UnsupportedOperationException;

}
