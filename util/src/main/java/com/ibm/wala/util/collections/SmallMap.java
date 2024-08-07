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

import static com.ibm.wala.util.nullability.NullabilityUtil.castToNonNull;
import static com.ibm.wala.util.nullability.NullabilityUtil.uncheckedNull;

import com.ibm.wala.util.debug.Assertions;
import com.uber.nullaway.annotations.EnsuresNonNull;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * A simple implementation of Map; intended for Maps with few elements. Optimized for space, not
 * time -- use with care.
 */
public class SmallMap<K, V extends @Nullable Object> implements Map<K, V> {

  private static final boolean DEBUG_USAGE = false;

  private static final int DEBUG_MAX_SIZE = 20;

  // this Map contains keysAndValues.length / 2 entries.
  // in the following array, entries 0 ... keysAndValues.length/2 - 1 are keys.
  // entries keysAndValues.length/2 .. keysAndValues.length are values.
  private @Nullable Object @Nullable [] keysAndValues;

  /*
   */
  @Override
  public int size() {
    if (keysAndValues == null) {
      return 0;
    } else {
      return keysAndValues.length / 2;
    }
  }

  /**
   * Use with care. Fails if key is null.
   *
   * @return the ith key
   */
  @SuppressWarnings("unchecked")
  public K getKey(int i) throws IllegalStateException {
    if (keysAndValues == null) {
      throw new IllegalStateException("getKey on empty map");
    }
    try {
      return (K) castToNonNull(keysAndValues[i]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid i: " + i, e);
    }
  }

  /**
   * Use with care.
   *
   * @return the ith key
   */
  @SuppressWarnings("unchecked")
  public V getValue(int i) throws IllegalStateException {
    if (keysAndValues == null) {
      throw new IllegalStateException("getValue on empty map");
    }
    try {
      // This can return null only when V gets instantiated as a @Nullable type.  But we cannot
      // express that in the type system; we can only mark the contents of keysAndValues as
      // @Nullable.  So we use an uncheckedNull() call.
      V theValue = (V) keysAndValues[size() + i];
      return theValue == null ? uncheckedNull() : theValue;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("illegal i: " + i, e);
    }
  }

  @Override
  public boolean isEmpty() {
    return (keysAndValues == null);
  }

  @Override
  public boolean containsKey(Object key) {
    if (keysAndValues == null) {
      return false;
    }
    for (int i = 0; i < size(); i++) {
      if (castToNonNull(keysAndValues[i]).equals(key)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    if (keysAndValues == null) {
      return false;
    }
    for (int i = size(); i < keysAndValues.length; i++) {
      Object v = keysAndValues[i];
      if (v == null) {
        if (value == null) {
          return true;
        }
      } else {
        if (v.equals(value)) {
          return true;
        }
      }
    }
    return false;
  }

  @NullUnmarked
  @Override
  @SuppressWarnings("unchecked")
  public V get(Object key) {

    if (key != null)
      for (int i = 0; i < size(); i++) {
        if (keysAndValues[i] != null && keysAndValues[i].equals(key)) {
          return (V) keysAndValues[size() + i];
        }
      }

    return null;
  }

  @EnsuresNonNull("keysAndValues")
  private void growByOne() {
    if (keysAndValues == null) keysAndValues = new Object[2];
    else {
      final int oldLength = keysAndValues.length;
      final int oldEntryCount = oldLength / 2;
      final int oldLastKeySlot = oldEntryCount - 1;
      final int oldFirstValueSlot = oldLastKeySlot + 1;

      final int newLength = oldLength + 2;
      final int newEntryCount = newLength / 2;
      final int newLastKeySlot = newEntryCount - 1;
      final int newFirstValueSlot = newLastKeySlot + 1;

      keysAndValues = Arrays.copyOf(keysAndValues, newLength);
      System.arraycopy(
          keysAndValues, oldFirstValueSlot, keysAndValues, newFirstValueSlot, oldEntryCount);
      keysAndValues[newLastKeySlot] = null;
    }
  }

  @Override
  @SuppressWarnings({"unchecked", "unused"})
  public @Nullable V put(Object key, Object value) {
    if (key == null) {
      throw new IllegalArgumentException("null key");
    }
    if (keysAndValues != null) {
      for (int i = 0; i < size(); i++) {
        if (keysAndValues[i] != null && keysAndValues[i].equals(key)) {
          V result = (V) keysAndValues[size() + i];
          keysAndValues[size() + i] = value;
          return result;
        }
      }
    }
    if (DEBUG_USAGE && size() >= DEBUG_MAX_SIZE) {
      Assertions.UNREACHABLE("too many elements in a SmallMap");
    }
    growByOne();
    keysAndValues[size() - 1] = key;
    keysAndValues[keysAndValues.length - 1] = value;
    return null;
  }

  @Override
  public V remove(Object key) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> t) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    keysAndValues = null;
  }

  @Override
  public Set<K> keySet() {
    //noinspection rawtypes
    return new SlotIteratingSet<>() {
      @Override
      protected K getItemInSlot(int slot) {
        return getKey(slot);
      }
    };
  }

  @Override
  public Collection<V> values() {
    //noinspection rawtypes
    return new SlotIteratingSet<>() {
      @Override
      protected V getItemInSlot(int slot) {
        return getValue(slot);
      }
    };
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    //noinspection rawtypes
    return new SlotIteratingSet<>() {
      @Override
      protected Entry<K, V> getItemInSlot(int slot) {
        return new AbstractMap.SimpleEntry<>(getKey(slot), getValue(slot));
      }
    };
  }

  /**
   * Minimally functional {@link Set} that iterates over array slots.
   *
   * @param <E> the type of elements maintained by this set
   */
  private abstract class SlotIteratingSet<E extends @Nullable Object> extends AbstractSet<E> {

    @Override
    public Iterator<E> iterator() {
      return new SlotIterator();
    }

    @Override
    public int size() {
      return SmallMap.this.size();
    }

    private class SlotIterator implements Iterator<E> {

      private int nextSlot = 0;

      @Override
      public boolean hasNext() {
        return nextSlot < SmallMap.this.size();
      }

      @Override
      public E next() {
        final E result;
        try {
          result = getItemInSlot(nextSlot);
        } catch (IllegalStateException | IllegalArgumentException problem) {
          throw new NoSuchElementException(problem.getMessage());
        }
        ++nextSlot;
        return result;
      }
    }

    protected abstract E getItemInSlot(int slot);
  }
}
