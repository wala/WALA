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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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

  @Override
  public Y next() {
    return f.apply(i.next());
  }

  @Override
  public boolean hasNext() {
    return i.hasNext();
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new java.lang.UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "map: " + f + " of " + i;
  }

  public static <X,Y> Iterator<Y> map(Function<X, Y> f, Iterator<X> i) {
    return new MapIterator<>(i, f);
  }

  public static <X,Y> Set<Y> map(Function<X, Y> f, Collection<X> i) {
    return Iterator2Collection.toSet(new MapIterator<>(i.iterator(), f));
  }
}
