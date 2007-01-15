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
 * 
 * Simple Heap data structure.
 * 
 * @author Julian Dolby\
 */

public abstract class Heap {

  /**
   * @param elt1
   * @param elt2
   * @return true iff elt1 is considered < elt2
   */
  abstract protected boolean compareElements(Object elt1, Object elt2);

  private int numberOfElements;

  private Object[] backingStore;

  /**
   * @return number of elements in this heap
   */
  public int size() {
    return numberOfElements;
  }

  public Heap(int initialCapacity) {
    numberOfElements = 0;
    backingStore = new Object[initialCapacity];
  }

  /**
   * @return true iff this heap is non-empty
   */
  final public boolean isEmpty() {
    return numberOfElements == 0;
  }

  /**
   * @param elt
   */
  public void insert(Object elt) {
    ensureCapacity(numberOfElements + 1);
    bubbleUp(elt, numberOfElements);
    numberOfElements++;
  }

  /**
   * @return the first object in the priority queue
   */
  public Object take() throws NoSuchElementException {
    if (numberOfElements == 0) {
      throw new NoSuchElementException();
    }
    Object result = backingStore[0];
    removeElement(0);
    return result;
  }

  private static int heapParent(int index) {
    return (index - 1) / 2;
  }

  private static int heapLeftChild(int index) {
    return index * 2 + 1;
  }

  private static int heapRightChild(int index) {
    return index * 2 + 2;
  }

  /**
   * @param min
   */
  final private void ensureCapacity(int min) {
    if (backingStore.length < min) {
      Object newStore[] = new Object[2 * min];
      System.arraycopy(backingStore, 0, newStore, 0, backingStore.length);
      backingStore = newStore;
    }
  }

  /**
   * SJF: I know this is horribly uglified ... I've attempted to make things as
   * easy as possible on the JIT, since this is performance critical.
   * 
   * @param index
   */
  final private void removeElement(int index) {
    int ne = numberOfElements;
    Object[] bs = backingStore;
    while (true) {
      int leftIndex = heapLeftChild(index);
      if (leftIndex < ne) {
        int rightIndex = heapRightChild(index);
        if (rightIndex < ne) {
          Object leftObject = bs[leftIndex];
          Object rightObject = bs[rightIndex];
          if (compareElements(leftObject, rightObject)) {
            bs[index] = leftObject;
            index = leftIndex;
          } else {
            bs[index] = rightObject;
            index = rightIndex;
          }
        } else {
          bs[index] = bs[leftIndex];
          index = leftIndex;
        }
        // manual tail recursion elimination here
      } else {
        numberOfElements--;
        ne = numberOfElements;
        if (index != ne) {
          bubbleUp(bs[ne], index);
        }
        return;
      }
    }
  }

  /**
   * SJF: I know this is uglified ... I've attempted to make things as easy as
   * possible on the JIT, since this is performance critical.
   * 
   * @param elt
   * @param index
   */
  final private void bubbleUp(Object elt, int index) {
    Object[] bs = backingStore;
    while (true) {
      if (index == 0) {
        bs[index] = elt;
        return;
      }
      int hpIndex = heapParent(index);
      Object parent = bs[hpIndex];
      if (compareElements(parent, elt)) {
        bs[index] = elt;
        return;
      } else {
        bs[index] = parent;
        // manual tail recursion elimination
        index = hpIndex;
      }
    }
  }
}
