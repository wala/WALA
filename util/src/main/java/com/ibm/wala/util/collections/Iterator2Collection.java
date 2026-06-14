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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Iterator;
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
  public static <T> Set<T> toSet(Iterator<? extends T> i) throws IllegalArgumentException {
    checkArgument(i != null, "i == null");
    Set<T> result = Sets.newLinkedHashSet();
    i.forEachRemaining(result::add);
    return result;
  }

  /** Returns a {@link List} containing all elements in i, preserving duplicates. */
  public static <T> List<T> toList(Iterator<? extends T> i) throws IllegalArgumentException {
    checkArgument(i != null, "i == null");
    return Lists.newArrayList(i);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public String toString() {
    return getDelegate().toString();
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public int size() {
    return getDelegate().size();
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public void clear() {
    getDelegate().clear();
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean isEmpty() {
    return getDelegate().isEmpty();
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public Object[] toArray() {
    return getDelegate().toArray();
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean add(T arg0) {
    return getDelegate().add(arg0);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean contains(Object arg0) {
    return getDelegate().contains(arg0);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean remove(Object arg0) {
    return getDelegate().remove(arg0);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean addAll(Collection<? extends T> arg0) {
    return getDelegate().addAll(arg0);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean containsAll(Collection<?> arg0) {
    return getDelegate().containsAll(arg0);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean removeAll(Collection<?> arg0) {
    return getDelegate().removeAll(arg0);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean retainAll(Collection<?> arg0) {
    return getDelegate().retainAll(arg0);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public Iterator<T> iterator() {
    return getDelegate().iterator();
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public <U> U[] toArray(U[] a) {
    return getDelegate().toArray(a);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public boolean equals(Object o) {
    return getDelegate().equals(o);
  }

  @Deprecated(forRemoval = true, since = "1.8.0")
  @Override
  public int hashCode() {
    return getDelegate().hashCode();
  }
}
