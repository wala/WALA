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
 * An <code>MapIterator</code> maps an
 * <code>Iterator</code> contents to produce a new Iterator
 * 
 * @author sfink
 */
public class MapIterator<X,Y> implements Iterator<Y> {
  final Iterator<? extends X> i;
  final Function<X,Y> f;

  /**
   * @param i
   * @param f
   */
  public MapIterator(Iterator<? extends X> i, Function<X,Y> f) {
    this.i = i;
    this.f = f;
  }


  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public Y next() {
    return f.apply(i.next());
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return i.hasNext();
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new java.lang.UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "map: " + f + " of " + i;
  }

}