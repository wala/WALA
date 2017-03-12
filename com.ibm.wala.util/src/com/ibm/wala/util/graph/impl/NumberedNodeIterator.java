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
import java.util.NoSuchElementException;

import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 */
public class NumberedNodeIterator<T> implements Iterator<T> {
  final IntIterator numbers;

  final NumberedNodeManager<T> nodeManager;

  /**
   * @throws IllegalArgumentException if s is null
   */
  public NumberedNodeIterator(IntSet s, NumberedNodeManager<T> nodeManager) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    this.numbers = s.intIterator();
    this.nodeManager = nodeManager;
  }

  @Override
  public boolean hasNext() {
    return numbers.hasNext();
  }

  @Override
  public T next() throws NoSuchElementException {
    int i = numbers.next();
    T result = nodeManager.getNode(i);
    assert result != null : "null node for " + i;
    return result;
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}
