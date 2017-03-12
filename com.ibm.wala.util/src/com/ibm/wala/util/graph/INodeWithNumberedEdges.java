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

import com.ibm.wala.util.intset.IntSet;

/**
 * Basic interface for a node which lives in one graph ... it's id is used to implement the {@link NumberedGraph} interface.
 */
public interface INodeWithNumberedEdges extends INodeWithNumber {
  /**
   * @return set of node numbers which are successors of this node
   */
  public IntSet getSuccNumbers();

  /**
   * @return set of node numbers which are predecessors of this node
   */
  public IntSet getPredNumbers();

  /**
   * Modify the graph so that node number n is a successor of this node
   */
  public void addSucc(int n);

  /**
   * Modify the graph so that node number n is a predecessor of this node
   */
  public void addPred(int n);

  /**
   * remove all edges that involve this node. This must fix up the other nodes involved in each edge removed.
   */
  public void removeAllIncidentEdges();

  /**
   * remove all incoming edges to this this node. This must fix up the other nodes involved in each edge removed.
   */
  public void removeIncomingEdges();

  /**
   * remove all outgoing edges to this this node. This must fix up the other nodes involved in each edge removed.
   */
  public void removeOutgoingEdges();

}
