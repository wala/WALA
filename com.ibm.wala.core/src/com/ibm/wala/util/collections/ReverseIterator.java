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
 *
 * An iterator that reverses an input iterator.
 * Not very efficient.
 * 
 * @author sfink
 */
public class ReverseIterator<T> implements Iterator<T> {

  final ArrayList<T> list = new ArrayList<T>();
  int nextIndex;
  
  /**
   * @param other the iterator to reverse
   */
  public ReverseIterator(Iterator<T> other) {
    while (other.hasNext()) {
      list.add(other.next());
    } 
    nextIndex = list.size() - 1;
  }
  
  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return nextIndex > -1;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public T next() throws NoSuchElementException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return list.get(nextIndex--);
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public static <T> Iterator<T> reverse(Iterator<T> it) {
    return new ReverseIterator<T>(it);
  }

}
