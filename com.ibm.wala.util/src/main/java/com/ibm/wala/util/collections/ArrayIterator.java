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
package com.ibm.wala.util.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** an Iterator of array elements */
public class ArrayIterator<T> implements Iterator<T> {

  /** The index of the next array element to return */
  protected int _cnt;

  /** The index of the last array element to return */
  protected final int last;

  /** The array source for the iterator */
  protected final T[] _elts;

  /** @param elts the array which should be iterated over */
  public ArrayIterator(T[] elts) {
    this(elts, 0);
  }

  /**
   * @param elts the array which should be iterated over
   * @param start the first array index to return
   */
  public ArrayIterator(T[] elts, int start) {
    if (elts == null) {
      throw new IllegalArgumentException("null elts");
    }
    if (start < 0 || start > elts.length) {
      throw new IllegalArgumentException(
          "invalid start: " + start + ", arrray length " + elts.length);
    }
    _elts = elts;
    _cnt = start;
    last = _elts.length - 1;
  }

  /**
   * @param elts the array which should be iterated over
   * @param start the first array index to return
   */
  public ArrayIterator(T[] elts, int start, int last) {
    if (elts == null) {
      throw new IllegalArgumentException("null elts");
    }
    if (start < 0) {
      throw new IllegalArgumentException("illegal start: " + start);
    }
    if (last < 0) {
      throw new IllegalArgumentException("illegal last: " + last);
    }
    _elts = elts;
    _cnt = start;
    this.last = last;
  }

  @Override
  public boolean hasNext() {
    return _cnt <= last;
  }

  @Override
  public T next() throws NoSuchElementException {
    if (_cnt >= _elts.length) {
      throw new NoSuchElementException();
    }
    return _elts[_cnt++];
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}
