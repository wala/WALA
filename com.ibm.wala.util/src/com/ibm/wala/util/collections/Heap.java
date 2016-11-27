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
 * Simple Heap data structure.
 */
public abstract class Heap<T> {

  /**
   * @return true iff elt1 is considered &lt; elt2
   */
  abstract protected boolean compareElements(T elt1, T elt2);

  private int numberOfElements;

  private T[] backingStore;

  /**
   * @return number of elements in this heap
   */
  public int size() {
    return numberOfElements;
  }

  @SuppressWarnings("unchecked")
  public Heap(int initialCapacity) {
    numberOfElements = 0;
    backingStore = (T[])new Object[initialCapacity];
  }

  /**
   * @return true iff this heap is non-empty
   */
  final public boolean isEmpty() {
    return numberOfElements == 0;
  }


  public void insert(T elt) {
    ensureCapacity(numberOfElements + 1);
    bubbleUp(elt, numberOfElements);
    numberOfElements++;
  }

  /**
   * @return the first object in the priority queue
   */
  public T take() throws NoSuchElementException {
    if (numberOfElements == 0) {
      throw new NoSuchElementException();
    }
    T result = backingStore[0];
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


  @SuppressWarnings("unchecked")
  final private void ensureCapacity(int min) {
    if (backingStore.length < min) {
      T newStore[] = (T[])new Object[2 * min];
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
    T[] bs = backingStore;
    while (true) {
      int leftIndex = heapLeftChild(index);
      if (leftIndex < ne) {
        int rightIndex = heapRightChild(index);
        if (rightIndex < ne) {
          T leftObject = bs[leftIndex];
          T rightObject = bs[rightIndex];
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
  final private void bubbleUp(T elt, int index) {
    T[] bs = backingStore;
    while (true) {
      if (index == 0) {
        bs[index] = elt;
        return;
      }
      int hpIndex = heapParent(index);
      T parent = bs[hpIndex];
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
  
  @Override
  public String toString() {
    StringBuffer s = new StringBuffer();
    s.append("[");
    for (int i = 0; i < size(); i++) {
      if (backingStore[i] != null) {
        if (i > 0)
          s.append(",");
        s.append(backingStore[i].toString());
      }
    }
    s.append("]");
    return s.toString();
  }
}
