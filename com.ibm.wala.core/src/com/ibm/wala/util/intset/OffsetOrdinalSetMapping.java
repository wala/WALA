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

import com.ibm.wala.util.debug.Assertions;

/**
 * An ordinal set mapping, backed a delegate, but adding an offset to each
 * index.
 * 
 * @author sjfink
 * 
 */
public class OffsetOrdinalSetMapping<T> implements OrdinalSetMapping<T> {

  private final OrdinalSetMapping<T> delegate;

  private final int offset;

  private OffsetOrdinalSetMapping(OrdinalSetMapping<T> delegate, int offset) {
    this.delegate = delegate;
    this.offset = offset;
  }

  public static <T> OffsetOrdinalSetMapping<T> make(OrdinalSetMapping<T> delegate, int offset) {
    return new OffsetOrdinalSetMapping<T>(delegate, offset);
  }

  public int add(T o) {
    return offset + delegate.add(o);
  }

  public int getMappedIndex(T o) {
    return offset + delegate.getMappedIndex(o);
  }

  public T getMappedObject(int n) throws NoSuchElementException {
    return delegate.getMappedObject(n - offset);
  }

  public int getMappingSize() {
    return delegate.getMappingSize();
  }

  public boolean hasMappedIndex(T o) {
    return delegate.hasMappedIndex(o);
  }

  public OrdinalSet<T> makeSingleton(int i) {
    Assertions.UNREACHABLE();
    return null;
  }

  public Iterator<T> iterator() {
    return delegate.iterator();
  }

}
