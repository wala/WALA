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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * FIFO work queue management of Objects that prevents an Object from being added to the queue if it
 * was ever previously enqueued.
 */
public class FifoQueueNoDuplicates<T> extends FifoQueue<T> {
  /**
   * Set representing items ever pushed into the work queue. This keeps an item from ever be pushed
   * again into the work queue.
   */
  private final Set<T> wasInQueue = HashSetFactory.make();

  /**
   * Return an Iterator over the set of all the nodes that were pushed into the queue.
   *
   * @return an Iterator over the set of pushed nodes.
   */
  public Iterator<T> getPushedNodes() {
    return wasInQueue.iterator();
  }

  /**
   * Insert an Object at the tail end of the queue if it was never pushed into the queue.
   *
   * <p>This method determines whether an element was ever in the queue using the element's {@code
   * equals()} method. If the element's class does not implement {@code equals()}, the default
   * implementation assumes they are equal if it is the same object.
   *
   * @param element is the Object to be added to the queue if not ever previously queued.
   */
  @Override
  public void push(T element) {
    if (wasInQueue.add(element)) {
      inQueue.add(element);
      qItems.add(element);
    }
  }

  /**
   * Insert all of the elements in the specified Iterator at the tail end of the queue if never
   * previously pushed into the queue.
   *
   * <p>This method determines whether an element was ever pushed into the queue using the element's
   * {@code equals()} method. If the element's class does not implement {@code equals()}, the
   * default implementation assumes that two elements are equal if they are the same object.
   *
   * @param elements an Iterator of Objects to be added to the queue if never already queued.
   * @throws IllegalArgumentException if elements == null
   */
  @Override
  public void push(Iterator<? extends T> elements) throws IllegalArgumentException {
    if (elements == null) {
      throw new IllegalArgumentException("elements == null");
    }
    while (elements.hasNext()) {
      T element = elements.next();
      if (wasInQueue.add(element)) {
        inQueue.add(element);
        qItems.add(element);
      }
    }
  }

  /**
   * Indicate whether the specified element was ever in the queue.
   *
   * @param element determine whether this object is in the queue.
   * @return {@code true} if {@code element} is in the queue. Otherwise {@code false}.
   */
  public boolean everContained(T element) {
    return wasInQueue.contains(element);
  }

  /** Return the set of objects that have been queued. */
  public Set<T> queuedSet() {
    return Collections.unmodifiableSet(wasInQueue);
  }
}
