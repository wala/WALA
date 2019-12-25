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

import java.util.NoSuchElementException;

/**
 * Iterator that only returns non-null elements of the array
 *
 * <p>hasNext() return true when there is a non-null element, false otherwise
 *
 * <p>next() returns the current element and advances the counter up to the next non-null element or
 * beyond the limit of the array
 */
public class ArrayNonNullIterator<T> extends ArrayIterator<T> {

  public ArrayNonNullIterator(T[] elts) {
    super(elts, 0);
  }

  public ArrayNonNullIterator(T[] elts, int start) {
    super(elts, start);
  }

  @Override
  public boolean hasNext() {
    return _cnt < _elts.length && _elts[_cnt] != null;
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    T result = _elts[_cnt];
    do {
      _cnt++;
    } while (_cnt < _elts.length && _elts[_cnt] == null);

    return result;
  }
}
