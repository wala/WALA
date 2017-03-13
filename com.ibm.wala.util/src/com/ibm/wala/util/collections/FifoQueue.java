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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * FIFO work queue management of Objects that prevents an object from being
 * added to the queue if it is already enqueued and has not yet been popped.
 */
public class FifoQueue<T> {
  /**
   * The work queue. Items are references to Object instances.
   */
  final List<T> qItems = new LinkedList<>();

  /**
   * Set representing items currently enqueue. This is used to keep an item from
   * having more than one entry in the queue.
   */
  final Set<T> inQueue = HashSetFactory.make();

  /**
   * Creates a FIFO queue with no elements enqueued.
   */
  public FifoQueue() {
  }

  /**
   * Creates a new FIFO queue containing the argument to this constructor.
   * 
   * @param element
   *          is the element to add to the queue.
   */
  public FifoQueue(T element) {
    push(element);
  }

  /**
   * Creates a new FIFO queue containing the elements of the specified
   * Collection. The order the elements are inserted into the queue is
   * unspecified.
   * 
   * @param collection
   *          is the Collection of Object instances to be enqueue.
   * @throws IllegalArgumentException  if collection is null
   */
  public FifoQueue(Collection<T> collection) {
    if (collection == null) {
      throw new IllegalArgumentException("collection is null");
    }
    push(collection.iterator());
  }

  /**
   * Return the current number of enqueued Objects, the number of Objects that
   * were pushed into the queue and have not been popped.
   * 
   * @return the current queue size.
   * @see #isEmpty
   */
  public int size() {
    return qItems.size();
  }

  /**
   * Returns whether or not this queue is empty (no enqueued elements).
   * 
   * @return <code>true</code> when there are no enqueued objects.
   *         <code>false</code> if there are objects remaining in the queue.
   * @see #size
   */
  public boolean isEmpty() {
    return qItems.isEmpty();
  }

  /**
   * Indicate whether the specified element is currently in the queue.
   * 
   * @param element
   *          determine whether this object is in the queue.
   * @return <code>true</code> if <code>element</code> is in the queue.
   *         Otherwise <code>false</code> if not currently in the queue.
   */
  public boolean contains(T element) {
    return inQueue.contains(element);
  }

  /**
   * Insert an Object at the tail end of the queue if it is not already in the
   * queue. If the Object is already in the queue, the queue remains unmodified.
   * <p>
   * This method determines whether an element is already in the queue using the
   * element's <code>equals()</code> method. If the element's class does not
   * implement <code>equals()</code>, the default implementation assumes they
   * are equal only if it is the same object.
   * 
   * @param element
   *          is the Object to be added to the queue if not already present in
   *          the queue.
   */
  public void push(T element) {
    // if element is not in inQueue, then add() returns true.
    if (inQueue.add(element)) {
      qItems.add(element);
    }
  }

  /**
   * Insert all of the elements in the specified Iterator at the tail end of the
   * queue if not already present in the queue. Any element in the Iterator
   * already in the queue is ignored.
   * <p>
   * This method determines whether an element is already in the queue using the
   * element's <code>equals()</code> method. If the element's class does not
   * implement <code>equals()</code>, the default implementation assumes they
   * are equal if it is the same object.
   * 
   * @param elements
   *          an Iterator of Objects to be added to the queue if not already
   *          queued.
   * @throws IllegalArgumentException  if elements == null
   */
  public void push(Iterator<? extends T> elements) throws IllegalArgumentException {
    if (elements == null) {
      throw new IllegalArgumentException("elements == null");
    }
    while (elements.hasNext()) {
      T element = elements.next();
      // if element is not in inQueue, then add() returns true.
      if (inQueue.add(element)) {
        qItems.add(element);
      }
    }
  }

  /**
   * Remove the next Object from the queue and return it to the caller. Throws
   * <code>IllegalStateException</code> if the queue is empty when this method
   * is called.
   * 
   * @return the next Object in the queue.
   */
  public T pop() throws IllegalStateException {
    // While there are work queue elements, remove the next element &
    // indicate that it is no longer in the work queue.
    // Throw a IllegalStateException when there is a queue underflow.
    if (isEmpty())
      throw new IllegalStateException("Unexpected empty queue during pop");

    // get & remove the top of the queue.
    T element = qItems.get(0);
    qItems.remove(0);

    // remove element from the elements that are 'inQueue'
    inQueue.remove(element);

    return element;
  }

  /**
   * Returns the next Object in the queue, but leaves it in the queue. Throws
   * <code>IllegalStateException</code> if the queue is empty when this method
   * is called.
   * 
   * @return the next Object in the queue.
   */
  public T peek() throws IllegalStateException {
    // While there are work queue elements, return the next element.
    // Throw a IllegalStateException if there is a queue underflow.
    if (isEmpty())
      throw new IllegalStateException("Unexpected empty queue during peek");

    // get & remove the top of the queue.
    return qItems.get(0);
  }
}
