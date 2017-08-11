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

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * A bit set mapping based on an immutable object array. This is not terribly efficient, but is useful for prototyping.
 */
public class ObjectArrayMapping<T> implements OrdinalSetMapping<T> {

  final private T[] array;

  /**
   * A mapping from object to Integer
   */
  final private HashMap<T, Integer> map = HashMapFactory.make();

  public ObjectArrayMapping(final T[] array) {
    if (array == null) {
      throw new IllegalArgumentException("null array");
    }
    this.array = array;
    for (int i = 0; i < array.length; i++) {
      map.put(array[i], Integer.valueOf(i));
    }
  }

  @Override
  public T getMappedObject(int n) throws NoSuchElementException {
    try {
      return array[n];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid n: " + n, e);
    }
  }

  @Override
  public int getMappedIndex(Object o) {
    if (map.get(o) == null) {
      return -1;
    }
    return map.get(o).intValue();
  }

  @Override
  public boolean hasMappedIndex(Object o) {
    return map.get(o) != null;
  }

  @Override
  public Iterator<T> iterator() {
    return map.keySet().iterator();
  }

  @Override
  public int add(Object o) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return 0;
  }

  @Override
  public int getMaximumIndex() {
    return array.length - 1;
  }

  @Override
  public int getSize() {
    return map.size();
  }
}
