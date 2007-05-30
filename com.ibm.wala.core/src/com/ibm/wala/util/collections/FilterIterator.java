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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A <code>FilterIterator</code> filters an <code>Iterator</code> to
 * generate a new one.
 * 
 * @author Mauricio J. Serrano
 * @author John Whaley
 * @author sfink
 */
public class FilterIterator<T> implements java.util.Iterator<T> {
  final Iterator<?> i;

  final Filter f;

  private T next = null;

  private boolean done = false;

  /**
   * @param i
   *          the original iterator
   * @param f
   *          a filter which defines which elements belong to the generated
   *          iterator
   */
  public FilterIterator(Iterator<?> i, Filter f) {
    this.i = i;
    this.f = f;
    advance();
  }

  /**
   * update the internal state to prepare for the next access to this iterator
   */
  @SuppressWarnings("unchecked")
  private void advance() {
    while (i.hasNext()) {
      Object o = i.next();
      if (f.accepts(o)) {
        next = (T) o;
        return;
      }
    }
    done = true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  @SuppressWarnings("unchecked")
  public T next() throws NoSuchElementException {
    if (done) {
      throw new java.util.NoSuchElementException();
    }
    T o = next;
    advance();
    return o;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return !done;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  public void remove() throws UnsupportedOperationException {
    throw new java.lang.UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "filter " + f + " of " + i;
  }
}