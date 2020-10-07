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

import com.ibm.wala.util.intset.IntIterator;
import java.util.Iterator;
import java.util.function.IntFunction;

/** An {@code IntMapIterator} maps an {@code Iterator} contents to produce a new Iterator */
public class IntMapIterator<T> implements Iterator<T> {
  final IntIterator i;

  final IntFunction<T> f;

  public IntMapIterator(IntIterator i, IntFunction<T> f) {
    if (i == null) {
      throw new IllegalArgumentException("null i");
    }
    if (f == null) {
      throw new IllegalArgumentException("null f");
    }
    this.i = i;
    this.f = f;
  }

  @Override
  public T next() {
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
}
