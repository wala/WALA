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
import java.util.LinkedHashSet;

/**
 * Converts an iterator to a collection
 * 
 * @author sfink
 */
public class Iterator2Collection<T> implements Collection<T> {

  private final Collection<T> delegate;


  private Iterator2Collection(Iterator<? extends T> i) {
    delegate = new LinkedHashSet<T>(5);
    while (i.hasNext()) {
      delegate.add(i.next());
    }
  }
  
  public static <T> Iterator2Collection<T> toCollection(Iterator<? extends T> i) {
    return new Iterator2Collection<T>(i);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
  
  /*
   * @see java.util.Collection#size()
   */
  public int size() {
    return delegate.size();
  }

  /*
   * @see java.util.Collection#clear()
   */
  public void clear() {
    delegate.clear();
  }

  /*
   * @see java.util.Collection#isEmpty()
   */
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  /*
   * @see java.util.Collection#toArray()
   */
  public Object[] toArray() {
    return delegate.toArray();
  }

  /*
   * @see java.util.Collection#add(java.lang.Object)
   */
  public boolean add(T arg0) {
    return delegate.add(arg0);
  }

  /*
   * @see java.util.Collection#contains(java.lang.Object)
   */
  public boolean contains(Object arg0) {
    return delegate.contains(arg0);
  }

  /*
   * @see java.util.Collection#remove(java.lang.Object)
   */
  public boolean remove(Object arg0) {
    return delegate.remove(arg0);
  }

  /*
   * @see java.util.Collection#addAll(java.util.Collection)
   */
  public boolean addAll(Collection<? extends T> arg0) {
    return delegate.addAll(arg0);
  }

  /*
   * @see java.util.Collection#containsAll(java.util.Collection)
   */
  public boolean containsAll(Collection<?> arg0) {
    return delegate.containsAll(arg0);
  }

  /*
   * @see java.util.Collection#removeAll(java.util.Collection)
   */
  public boolean removeAll(Collection<?> arg0) {
    return delegate.removeAll(arg0);
  }

  /*
   * @see java.util.Collection#retainAll(java.util.Collection)
   */
  public boolean retainAll(Collection<?> arg0) {
    return delegate.retainAll(arg0);
  }

  /*
   * @see java.util.Collection#iterator()
   */
  public Iterator<T> iterator() {
    return delegate.iterator();
  }

  @SuppressWarnings("hiding")
  public <T> T[] toArray(T[] a) {
    return delegate.toArray(a);
  }

  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}
