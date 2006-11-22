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
package com.ibm.wala.util;

import java.util.Iterator;

/**
 *
 * An iterator which provides a logical concatenation of the lists from two other iterators
 * 
 * @author sfink
 */
public class CompoundIterator<T> implements Iterator<T> {

  Iterator<? extends T> A;
  Iterator<? extends T> B;
  /**
   * Constructor for CompoundIterator.
   */
  public CompoundIterator(Iterator<? extends T> A, Iterator<? extends T> B) {
    this.A = A;
    this.B = B;
  }

  /**
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return A.hasNext() || B.hasNext();
  }

  /**
   * @see java.util.Iterator#next()
   */
  public T next() {
    if (A.hasNext()) {
      return A.next();
    } else {
      return B.next();
    }
  }

  /**
   * @see java.util.Iterator#remove()
   */
  public void remove() {
  }

}
