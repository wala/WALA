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
package com.ibm.wala.util.intset;

import static com.ibm.wala.util.nullability.NullabilityUtil.uncheckedNull;

import com.ibm.wala.util.collections.HashMapFactory;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A bit set mapping based on an object array. This is not terribly efficient, but is useful for
 * prototyping.
 */
public class MutableMapping<T extends @Nullable Object>
    implements OrdinalSetMapping<T>, Serializable {

  private static final long serialVersionUID = 4011751404163534418L;

  private static final int INITIAL_CAPACITY = 20;

  private static final int MAX_SIZE = Integer.MAX_VALUE / 4;

  public static <T> MutableMapping<T> make() {
    return new MutableMapping<>();
  }

  private @Nullable Object[] array;

  private int nextIndex;

  /** A mapping from object to Integer. */
  final HashMap<T, Integer> map = HashMapFactory.make();

  /**
   * @throws IllegalArgumentException if array is null
   */
  @SuppressWarnings("unchecked")
  public MutableMapping(final Object[] array) {
    if (array == null) {
      throw new IllegalArgumentException("array is null");
    }
    this.array = new Object[2 * array.length];
    for (int i = 0; i < array.length; i++) {
      this.array[i] = array[i];
      map.put((T) array[i], i);
    }
    nextIndex = array.length;
  }

  protected MutableMapping() {
    array = new Object[INITIAL_CAPACITY];
    nextIndex = 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T getMappedObject(int n) {
    try {
      // should only return null when T is @Nullable but we can't express that in the
      // NullAway type system.  Work around using uncheckedNull().
      T result = (T) array[n];
      return result == null ? uncheckedNull() : result;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("n out of range " + n, e);
    }
  }

  @Override
  public int getMappedIndex(@Nullable Object o) {
    Integer I = map.get(o);
    if (I == null) {
      return -1;
    } else {
      return I;
    }
  }

  @Override
  public boolean hasMappedIndex(T o) {
    return map.get(o) != null;
  }

  /**
   * Add an object to the set of mapped objects.
   *
   * @return the integer to which the object is mapped.
   */
  @Override
  public int add(T o) {
    Integer I = map.get(o);
    if (I != null) {
      return I;
    }
    map.put(o, nextIndex);
    if (nextIndex >= array.length) {
      array = Arrays.copyOf(array, 2 * array.length);
    }
    int result = nextIndex++;
    array[result] = o;
    return result;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < nextIndex; i++) {
      result.append(i).append("  ").append(array[i]).append('\n');
    }
    return result.toString();
  }

  /**
   * @see com.ibm.wala.util.intset.OrdinalSetMapping#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    return map.keySet().iterator();
  }

  @Override
  public Stream<T> stream() {
    return map.keySet().stream();
  }

  /**
   * @see com.ibm.wala.util.intset.SparseIntSet#singleton(int)
   */
  public OrdinalSet<T> makeSingleton(int i) {
    return new OrdinalSet<>(SparseIntSet.singleton(i), this);
  }

  public void deleteMappedObject(T n) {
    int index = getMappedIndex(n);
    if (index != -1) {
      array[index] = null;
      map.remove(n);
    }
  }

  public Collection<T> getObjects() {
    return Collections.unmodifiableCollection(map.keySet());
  }

  /** Replace a in this mapping with b. */
  public void replace(T a, T b) throws IllegalArgumentException {
    int i = getMappedIndex(a);
    if (i == -1) {
      throw new IllegalArgumentException("first element does not exist in map");
    }
    map.remove(a);
    map.put(b, i);
    array[i] = b;
  }

  /** Add an object to the set of mapped objects at index i. */
  public void put(int i, T o) {

    if (i < 0 || i > MAX_SIZE) {
      throw new IllegalArgumentException("invalid i: " + i);
    }
    Integer I = i;
    map.put(o, I);
    if (i >= array.length) {
      array = Arrays.copyOf(array, 2 * i);
    }
    array[i] = o;
    nextIndex = Math.max(nextIndex, i + 1);
  }

  @Override
  public int getMaximumIndex() {
    return nextIndex - 1;
  }

  @Override
  public int getSize() {
    return map.size();
  }
}
