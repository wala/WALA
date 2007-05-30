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

import com.ibm.wala.util.graph.impl.NumberedNodeIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * 
 * Basic functionality for a graph that delegates node and edge management, and
 * tracks node numbers
 * 
 * @author sfink
 */
public abstract class AbstractNumberedGraph<T> extends AbstractGraph<T> implements NumberedGraph<T> {

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getMaxNumber()
   */
  public int getMaxNumber() {
    return ((NumberedNodeManager<T>) getNodeManager()).getMaxNumber();
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getNode(int)
   */
  public T getNode(int number) {
    return ((NumberedNodeManager<T>) getNodeManager()).getNode(number);
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getNumber(com.ibm.wala.util.graph.Node)
   */
  public int getNumber(T N) {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    return ((NumberedNodeManager<T>) getNodeManager()).getNumber(N);
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
   */
  public Iterator<T> iterateNodes(final IntSet s) {
    return new NumberedNodeIterator<T>(s, this);
  }
  
  public IntSet getPredNodeNumbers(T node) throws IllegalArgumentException {
    return ((NumberedEdgeManager<T>) getEdgeManager()).getPredNodeNumbers(node);
  }

  public IntSet getSuccNodeNumbers(T node) throws IllegalArgumentException {
    return ((NumberedEdgeManager<T>) getEdgeManager()).getSuccNodeNumbers(node);
  }
}