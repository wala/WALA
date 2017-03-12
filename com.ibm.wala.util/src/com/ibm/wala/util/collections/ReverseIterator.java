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
package com.ibm.wala.util.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that reverses an input iterator. Not very efficient.
 */
public class ReverseIterator<T> implements Iterator<T> {

  final ArrayList<T> list = new ArrayList<>();

  int nextIndex;

  /**
   * @param other
   *          the iterator to reverse
   * @throws IllegalArgumentException  if other == null
   */
  private ReverseIterator(Iterator<T> other) throws IllegalArgumentException {
    if (other == null) {
      throw new IllegalArgumentException("other == null");
    }
    while (other.hasNext()) {
      list.add(other.next());
    }
    nextIndex = list.size() - 1;
  }

  @Override
  public boolean hasNext() {
    return nextIndex > -1;
  }

  @Override
  public T next() throws NoSuchElementException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return list.get(nextIndex--);
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public static <T> Iterator<T> reverse(Iterator<T> it) {
    return new ReverseIterator<>(it);
  }

}
