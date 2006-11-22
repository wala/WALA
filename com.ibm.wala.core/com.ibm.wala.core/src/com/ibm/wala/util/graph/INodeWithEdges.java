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
 * Basic interface for a node in a directed graph.
 * 
 * @author Aaron Kershenbaum
 * @author Larry Koved
 * @author Stephen Fink
 */

public interface INodeWithEdges {

  /**
   * Return an Iterator over the immediate predecessor {@link Node nodes}of
   * this Node in the Graph.
   * 
   * This method never returns <code>null</code>.
   * 
   * @return an Iterator over the immediate predecessor nodes of this Node.
   */
  public Iterator getPredNodes();

  /**
   * Return the number of {@link #getPredNodes immediate predecessor}
   * {@link Node nodes}of this Node in the Graph.
   * 
   * @return the number of immediate predecessor Nodes of this Node in the
   *         Graph.
   */
  public int getPredNodeCount();

  /**
   * Return an Iterator over the immediate successor {@link Node nodes}of this
   * Node in the Graph
   * <p>
   * This method never returns <code>null</code>.
   * 
   * @return an Iterator over the immediate successor Nodes of this Node.
   */
  public Iterator getSuccNodes();

  /**
   * Return the number of {@link #getSuccNodes immediate successor}
   * {@link Node nodes}of this Node in the Graph
   * 
   * @return the number of immediate successor Nodes of this Node in the Graph.
   */
  public int getSuccNodeCount();

  /**
   * remove all edges that involve this node. This must fix up the other nodes
   * involved in each edge removed.
   */
  public void removeAllIncidentEdges();

  /**
   * remove all incoming edges to this this node. This must fix up the other
   * nodes involved in each edge removed.
   */
  public void removeIncomingEdges();

  /**
   * remove all outgoing edges to this this node. This must fix up the other
   * nodes involved in each edge removed.
   */
  public void removeOutgoingEdges();

}