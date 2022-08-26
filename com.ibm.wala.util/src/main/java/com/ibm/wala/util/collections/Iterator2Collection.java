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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts an {@link Iterator} to a {@link Collection}. Note that if you just want to use Java 5's
 * for-each loop with an {@link Iterator}, use {@link Iterator2Iterable}.
 *
 * @see Iterator2Iterable
 */
public abstract class Iterator2Collection<T> implements Collection<T> {

  protected abstract Collection<T> getDelegate();
  /** Returns a {@link Set} containing all elements in i. Note that duplicates will be removed. */
  public static <T> Iterator2Set<T> toSet(Iterator<? extends T> i) throws IllegalArgumentException {
    if (i == null) {
      throw new IllegalArgumentException("i == null");
    }
    return new Iterator2Set<>(i, new LinkedHashSet<>(5));
  }

  /** Returns a {@link List} containing all elements in i, preserving duplicates. */
  public static <T> Iterator2List<T> toList(Iterator<? extends T> i)
      throws IllegalArgumentException {
    if (i == null) {
      throw new IllegalArgumentException("i == null");
    }
    return new Iterator2List<>(i, new ArrayList<>(5));
  }

  @Override
  public String toString() {
    return getDelegate().toString();
  }

  @Override
  public int size() {
    return getDelegate().size();
  }

  @Override
  public void clear() {
    getDelegate().clear();
  }

  @Override
  public boolean isEmpty() {
    return getDelegate().isEmpty();
  }

  @Override
  public Object[] toArray() {
    return getDelegate().toArray();
  }

  @Override
  public boolean add(T arg0) {
    return getDelegate().add(arg0);
  }

  @Override
  public boolean contains(Object arg0) {
    return getDelegate().contains(arg0);
  }

  @Override
  public boolean remove(Object arg0) {
    return getDelegate().remove(arg0);
  }

  @Override
  public boolean addAll(Collection<? extends T> arg0) {
    return getDelegate().addAll(arg0);
  }

  @Override
  public boolean containsAll(Collection<?> arg0) {
    return getDelegate().containsAll(arg0);
  }

  @Override
  public boolean removeAll(Collection<?> arg0) {
    return getDelegate().removeAll(arg0);
  }

  @Override
  public boolean retainAll(Collection<?> arg0) {
    return getDelegate().retainAll(arg0);
  }

  @Override
  public Iterator<T> iterator() {
    return getDelegate().iterator();
  }

  @Override
  public <U> U[] toArray(U[] a) {
    return getDelegate().toArray(a);
  }

  @Override
  public boolean equals(Object o) {
    return getDelegate().equals(o);
  }

  @Override
  public int hashCode() {
    return getDelegate().hashCode();
  }
}
