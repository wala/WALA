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
package com.ibm.wala.util.graph.impl;

import java.util.Iterator;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * @author sfink
 *
 */
public class NumberedNodeIterator<T> implements Iterator<T> {
  final IntIterator numbers;
  final NumberedNodeManager<T> nodeManager;

  /**
   * @param s
   * @param nodeManager
   */
  public NumberedNodeIterator(IntSet s, NumberedNodeManager<T> nodeManager) {
    this.numbers = s.intIterator();
    this.nodeManager = nodeManager;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return numbers.hasNext();
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public T next() {
    int i  = numbers.next();
    T result = nodeManager.getNode(i);
    if (Assertions.verifyAssertions) {
      if (result == null) {
        Assertions._assert(result != null, "null node for " + i);
      }
    }
    return result;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}