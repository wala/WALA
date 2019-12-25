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

/**
 * Basic interface for a node which lives in one graph ... it's id is used to implement the {@link
 * NumberedGraph} interface.
 */
public interface INodeWithNumber {

  /**
   * A non-negative integer which serves as an identifier for this node in it's "dominant" graph.
   * Initially this number is -1; a NumberedGraph will set it to a non-negative value when this node
   * is inserted into the graph
   *
   * @return the identifier
   */
  public int getGraphNodeId();

  void setGraphNodeId(int number);
}
