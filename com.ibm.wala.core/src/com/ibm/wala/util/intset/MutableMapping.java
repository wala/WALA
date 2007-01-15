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
package com.ibm.wala.util.intset;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.wala.util.collections.HashMapFactory;

/**
 *
 * A bit set mapping based on an object array.
 * This is not terribly efficient, but is useful for prototyping.
 * 
 * @author sfink
 */
public class MutableMapping<T> implements OrdinalSetMapping<T> {

  private static final int INITIAL_CAPACITY = 20;

  private Object[] array;
  private int nextIndex = 0;

  /**
   * A mapping from object to Integer.
   */
  HashMap<T, Integer> map = HashMapFactory.make();

  /**
   * Constructor for MutableMapping.
   */
  @SuppressWarnings("unchecked")
  public MutableMapping(final Object[] array) {
    this.array = new Object[2 * array.length];
    for (int i = 0; i < array.length; i++) {
      this.array[i] = array[i];
      map.put((T) array[i], new Integer(i));
    }
    nextIndex = array.length;
  }

  /**
   * Constructor MutableMapping.
   */
  public MutableMapping() {
    array = new Object[INITIAL_CAPACITY];
    nextIndex = 0;
  }

  /**
   */
  @SuppressWarnings("unchecked")
  public T getMappedObject(int n) {
    return (T) array[n];
  }

 
  /* (non-Javadoc)
   */
  public int getMappedIndex(Object o) {
    Integer I = map.get(o);
    if (I == null) {
      return -1;
    } else {
      return I.intValue();
    }

  }

  public boolean hasMappedIndex(Object o) {
    return map.get(o) != null;
  }

  /**
   */
  public int getMappingSize() {
    return nextIndex;
  }

  /**
   * Add an Object to the set of mapped objects.
   * @return the integer to which the object is mapped.
   */
  public int add(T o) {
    Integer I = map.get(o);
    if (I != null) {
      return I.intValue();
    }
    map.put(o, new Integer(nextIndex));
    if (nextIndex >= array.length) {
      Object[] old = array;
      array = new Object[2 * array.length];
      System.arraycopy(old, 0, array, 0, old.length);
    }
    int result = nextIndex++;
    array[result] = o;
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i<nextIndex; i++) {
      result.append(i).append("  ").append(array[i]).append("\n");
    }
    return result.toString();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.intset.OrdinalSetMapping#iterator()
   */
  public Iterator<T> iterator() {
    return map.keySet().iterator();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.intset.OrdinalSetMapping#makeSingleton(int)
   */
  public OrdinalSet<T> makeSingleton(int i) {
    return new OrdinalSet<T>(SparseIntSet.singleton(i),this);
  }

  /**
   * @param n
   */
  public void deleteMappedObject(Object n) {
    int index = getMappedIndex(n);
    if (index != -1) {
      array[index] = null;
      map.remove(n);
    }
  }
  
  public Collection<T> getObjects() {
    return Collections.unmodifiableCollection(map.keySet());
  }

  /**
   * Replace a in this mapping with b.
   */
  public void replace(T a, T b) throws IllegalArgumentException {
    int i = getMappedIndex(a);
    if (i == -1) {
      throw new IllegalArgumentException("first element does not exist in map");
    }
    map.remove(a);
    map.put(b,new Integer(i));
    array[i] = b; 
  }

}
