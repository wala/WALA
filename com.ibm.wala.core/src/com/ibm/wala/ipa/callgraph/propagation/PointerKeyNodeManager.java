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
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Iterator;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.NodeManager;

/**
 * 
 * An object that tracks nodes in a graph, where each node is a PointerKey
 * represented in the input pointsToSets
 * 
 * @author Julian Dolby
 * @author sfink
 */
public class PointerKeyNodeManager implements NodeManager {

  /**
   * a mapping from PointerKey -> IntSetVariable
   */
  private final PointsToMap pointsToMap;

  /**
   * number of nodes in this graph
   */
  private int count = 0;

  public PointerKeyNodeManager(PointsToMap pointsToMap) {
    this.pointsToMap = pointsToMap;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
   */
  public Iterator iterateNodes() {
    return pointsToMap.iterateKeys();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
   */
  public int getNumberOfNodes() {
    if (count == 0) {
      countNodes();
    }

    return count;
  }

  /**
   * Count the number of nodes in this graph
   */
  private void countNodes() {
    for (Iterator x = iterateNodes(); x.hasNext(); x.next(), count++)
      ;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
   */
  public void addNode(Object n) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
   */
  public void removeNode(Object n) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
   */
  public boolean containsNode(Object n) {
    return pointsToMap.getPointsToSet((PointerKey) n) != null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    int n = 0;
    StringBuffer s = new StringBuffer("dataflow nodes:\n");
    for (Iterator i = iterateNodes(); i.hasNext();)
      s.append(n++ + ":\t " + i.next() + "\n");

    return s.toString();
  }

}
