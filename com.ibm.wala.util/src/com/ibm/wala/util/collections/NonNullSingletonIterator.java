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
 * A singleton iterator for an object which is guaranteed to be not-null.  Exploiting this invariant
 * allows this class to be slightly more efficient than Collections.iterator()
 */
public class NonNullSingletonIterator<T> implements Iterator<T> {

  private T it;

  /**
   * @param o the single object in this collection, must be non-null
   */
  public NonNullSingletonIterator(T o) {
    if (o == null) {
      throw new IllegalArgumentException("o is null");
    }
    this.it = o;
  }

  @Override
  public boolean hasNext() {
    return it != null;
  }

  @Override
  public T next() {
    if (it == null) {
      throw new NoSuchElementException();
    } else {
      T result = it;
      it = null;
      return result;
    }
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public static <T> NonNullSingletonIterator<T> make(T item) {
    return new NonNullSingletonIterator<>(item);
  }

}
