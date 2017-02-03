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

/**
 * A 2-level iterator.  has not been tested yet!
 */
public abstract class ComposedIterator<O,I> implements Iterator<I> {

  private final Iterator<O> outer;
  private Iterator<? extends I> inner;
  public ComposedIterator(Iterator<O> outer) {
    this.outer = outer;
    advanceOuter();
  }

  private void advanceOuter() {
    while (outer.hasNext()) {
      inner = makeInner(outer.next());
      if (inner.hasNext()) {
        break;
      }
    }
    if (inner != null && !inner.hasNext()) {
      inner = null;
    }
  }
  
  public abstract Iterator<? extends I> makeInner(O outer);
  
  @Override
  public void remove() throws UnsupportedOperationException{
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasNext() {
    return (inner != null);
  }

  @Override
  public I next() {
    I result = inner.next();
    if (!inner.hasNext()) {
      advanceOuter();
    }
    return result;
  }
  
}
