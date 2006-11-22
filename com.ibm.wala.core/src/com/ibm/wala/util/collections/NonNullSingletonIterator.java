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

import com.ibm.wala.util.debug.Assertions;

/**
 *
 * A singleton iterator for an object which is guaranteed to be not-null.  Exploiting this invariant
 * allows this class to be slightly more efficient than Collections.iterator()
 * 
 * @author sfink
 */
public class NonNullSingletonIterator<T> implements Iterator<T> {

  private T it;


  /**
   * @param o the single object in this collection, must be non-null
   */
  public NonNullSingletonIterator(T o) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(o != null);
    }
    this.it = o;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return it != null;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public T next() {
    if (it == null) {
      throw new NoSuchElementException();
    } else {
      T result = it;
      it = null;
      return result;
    }
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    Assertions.UNREACHABLE();
  }

}
