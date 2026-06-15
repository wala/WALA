/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@Deprecated(forRemoval = true, since = "1.8.0")
public class Iterator2List<T> extends Iterator2Collection<T> implements Serializable, List<T> {

  @Serial private static final long serialVersionUID = -4364941553982190713L;

  private final List<T> delegate;

  @Deprecated(forRemoval = true, since = "1.8.0")
  public Iterator2List(Iterator<? extends T> i, List<T> delegate) {
    this.delegate = delegate;
    i.forEachRemaining(delegate::add);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public void add(int index, T element) {
    delegate.add(index, element);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    return delegate.addAll(index, c);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public T get(int index) {
    return delegate.get(index);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public int indexOf(Object o) {
    return delegate.indexOf(o);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public int lastIndexOf(Object o) {
    return delegate.lastIndexOf(o);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public ListIterator<T> listIterator() {
    return delegate.listIterator();
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public ListIterator<T> listIterator(int index) {
    return delegate.listIterator(index);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public T remove(int index) {
    return delegate.remove(index);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public T set(int index, T element) {
    return delegate.set(index, element);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return delegate.subList(fromIndex, toIndex);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  protected Collection<T> getDelegate() {
    return delegate;
  }
}
