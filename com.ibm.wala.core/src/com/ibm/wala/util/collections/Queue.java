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

import java.util.NoSuchElementException;

/**
 * @author unknown
 * 
 * TODO: document me!
 */
public class Queue<T> {

  private Object[] items;

  private int first = -1;

  private int free = 0;

  public Queue(int initialSize) {
    items = new Object[initialSize];
  }

  public Queue() {
    this(10);
  }

  private int inc(int n) {
    return (n + 1) % items.length;
  }

  private boolean isFull() {
    return first == free;
  }

  private void resize() {
    Object[] newItems = new Object[items.length * 2 + 1];
    System.arraycopy(items, first, newItems, 0, items.length - first);
    System.arraycopy(items, 0, newItems, items.length - first, first);
    first = 0;
    free = items.length;
    items = newItems;
  }

  public T enqueue(T item) {
    if (isFull())
      resize();

    items[free] = item;

    if (first == -1)
      first = free;
    free = inc(free);

    return item;
  }

  @SuppressWarnings("unchecked")
  public T dequeue() throws NoSuchElementException {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }

    T result = (T) items[first];

    first = inc(first);
    if (first == free)
      first = -1;

    return result;
  }

  public boolean isEmpty() {
    return first == -1;
  }
}