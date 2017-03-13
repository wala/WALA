/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.intset;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An ordinal set mapping, backed a delegate, but adding an offset to each index.
 */
public class OffsetOrdinalSetMapping<T> implements OrdinalSetMapping<T> {

  private final OrdinalSetMapping<T> delegate;

  private final int offset;

  private OffsetOrdinalSetMapping(OrdinalSetMapping<T> delegate, int offset) {
    this.delegate = delegate;
    this.offset = offset;
  }

  @Override
  public int getMaximumIndex() {
    return offset + delegate.getMaximumIndex();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  public static <T> OffsetOrdinalSetMapping<T> make(OrdinalSetMapping<T> delegate, int offset) {
    if (delegate == null) {
      throw new IllegalArgumentException("null delegate");
    }
    return new OffsetOrdinalSetMapping<>(delegate, offset);
  }

  public static <T> OffsetOrdinalSetMapping<T> make(int offset) {
    MutableMapping<T> m = MutableMapping.make();
    return new OffsetOrdinalSetMapping<>(m, offset);
  }

  @Override
  public int add(T o) {
    return offset + delegate.add(o);
  }

  @Override
  public int getMappedIndex(Object o) {
    if (delegate.getMappedIndex(o) == -1) {
      return -1;
    }
    return offset + delegate.getMappedIndex(o);
  }

  @Override
  public T getMappedObject(int n) throws NoSuchElementException {
    return delegate.getMappedObject(n - offset);
  }

  @Override
  public boolean hasMappedIndex(T o) {
    return delegate.hasMappedIndex(o);
  }

  @Override
  public Iterator<T> iterator() {
    return delegate.iterator();
  }

}
