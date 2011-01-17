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
package com.ibm.wala.util.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Iterator2List<T> extends Iterator2Collection<T> implements List<T> {

  private final List<T> delegate;

  public Iterator2List(Iterator<? extends T> i, List<T> delegate) {
    this.delegate = delegate;
    while (i.hasNext()) {
      delegate.add(i.next());
    }
  }

  public void add(int index, T element) {
    delegate.add(index, element);
  }

  public boolean addAll(int index, Collection<? extends T> c) {
    return delegate.addAll(index, c);
  }

  public T get(int index) {
    return delegate.get(index);
  }

  public int indexOf(Object o) {
    return delegate.indexOf(o);
  }

  public int lastIndexOf(Object o) {
    return delegate.lastIndexOf(o);
  }

  public ListIterator<T> listIterator() {
    return delegate.listIterator();
  }

  public ListIterator<T> listIterator(int index) {
    return delegate.listIterator(index);
  }

  public T remove(int index) {
    return delegate.remove(index);
  }

  public T set(int index, T element) {
    return delegate.set(index, element);
  }

  public List<T> subList(int fromIndex, int toIndex) {
    return delegate.subList(fromIndex, toIndex);
  }

  @Override
  protected Collection<T> getDelegate() {
    return delegate;
  }

}
