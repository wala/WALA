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

import com.ibm.wala.util.Predicate;

/**
 * A <code>FilterIterator</code> filters an <code>Iterator</code> to generate a new one.
 */
public class FilterIterator<T> implements java.util.Iterator<T> {
  final Iterator<?> i;

  @SuppressWarnings("rawtypes")
  final Predicate f;

  private T next = null;

  private boolean done = false;

  /**
   * @param i the original iterator
   * @param f a filter which defines which elements belong to the generated iterator
   */
  @SuppressWarnings("rawtypes")
  public FilterIterator(Iterator<?> i, Predicate f) {
    if (i == null) {
      throw new IllegalArgumentException("null i");
    }
    if (f == null) {
      throw new IllegalArgumentException("null f");
    }
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
      if (f.test(o)) {
        next = (T) o;
        return;
      }
    }
    done = true;
  }

  @Override
  public T next() throws NoSuchElementException {
    if (done) {
      throw new java.util.NoSuchElementException();
    }
    T o = next;
    advance();
    return o;
  }

  @Override
  public boolean hasNext() {
    return !done;
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new java.lang.UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "filter " + f + " of " + i;
  }
}
