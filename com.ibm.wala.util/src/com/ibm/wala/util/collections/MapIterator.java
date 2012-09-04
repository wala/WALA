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

import com.ibm.wala.util.functions.Function;

/**
 * An <code>MapIterator</code> maps an <code>Iterator</code> contents to produce a new Iterator
 */
public class MapIterator<X, Y> implements Iterator<Y> {
  final Iterator<? extends X> i;

  final Function<X, Y> f;

  public MapIterator(Iterator<? extends X> i, Function<X, Y> f) {
    if (i == null) {
      throw new IllegalArgumentException("null i");
    }
    this.i = i;
    this.f = f;
  }

  public Y next() {
    return f.apply(i.next());
  }

  public boolean hasNext() {
    return i.hasNext();
  }

  public void remove() throws UnsupportedOperationException {
    throw new java.lang.UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "map: " + f + " of " + i;
  }

}