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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.debug.UnimplementedError;

/**
 * This implementation of {@link Map} chooses between one of two implementations, depending on the size of the map.
 */
public class BimodalMap<K, V> implements Map<K, V> {

  // what's the cutoff between small and big maps?
  // this may be a time-space tradeoff; the caller must determine if
  // it's willing to put up with slower random access in exchange for
  // smaller footprint.
  private final int cutOff;

  /**
   * The implementation we delegate to
   */
  private Map<K, V> backingStore;

  /**
   * @param cutoff the map size at which to switch from the small map implementation to the large map implementation
   */
  public BimodalMap(int cutoff) {
    this.cutOff = cutoff;
  }

  /*
   * @see java.util.Map#size()
   */
  @Override
  public int size() {
    return (backingStore == null) ? 0 : backingStore.size();
  }

  /*
   * @see java.util.Map#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return (backingStore == null) ? true : backingStore.isEmpty();
  }

  /*
   * @see java.util.Map#containsKey(java.lang.Object)
   */
  @Override
  public boolean containsKey(Object key) {
    return (backingStore == null) ? false : backingStore.containsKey(key);
  }

  /*
   * @see java.util.Map#containsValue(java.lang.Object)
   */
  @Override
  public boolean containsValue(Object value) {
    return (backingStore == null) ? false : backingStore.containsValue(value);
  }

  /*
   * @see java.util.Map#get(java.lang.Object)
   */
  @Override
  public V get(Object key) {
    return (backingStore == null) ? null : backingStore.get(key);
  }

  /*
   * @see java.util.Map#put(java.lang.Object, java.lang.Object)
   */
  @Override
  public V put(K key, V value) {
    if (backingStore == null) {
      backingStore = new SmallMap<>();
      backingStore.put(key, value);
      return null;
    } else {
      if (backingStore instanceof SmallMap) {
        V result = backingStore.put(key, value);
        if (backingStore.size() > cutOff) {
          transferBackingStore();
        }
        return result;
      } else {
        return backingStore.put(key, value);
      }
    }
  }

  /**
   * Switch backing implementation from a SmallMap to a HashMap
   */
  private void transferBackingStore() {
    assert backingStore instanceof SmallMap;
    SmallMap<K, V> S = (SmallMap<K, V>) backingStore;
    backingStore = HashMapFactory.make(2 * S.size());
    for (K key : S.keySet()) {
      V value = S.get(key);
      backingStore.put(key, value);
    }
  }

  /**
   * @throws UnsupportedOperationException if the backingStore doesn't support remove
   */
  @Override
  public V remove(Object key) {
    return (backingStore == null) ? null : backingStore.remove(key);
  }

  /*
   * @see java.util.Map#putAll(java.util.Map)
   */
  @Override
  @SuppressWarnings("unchecked")
  public void putAll(Map<? extends K, ? extends V> t) throws UnsupportedOperationException {
    if (t == null) {
      throw new IllegalArgumentException("null t");
    }
    if (backingStore == null) {
      int size = t.size();
      if (size > cutOff) {
        backingStore = HashMapFactory.make();
      } else {
        backingStore = new SmallMap<>();
      }
      backingStore.putAll(t);
      return;
    } else {
      if (backingStore instanceof SmallMap) {
        if (t.size() > cutOff) {
          Map<K, V> old = backingStore;
          backingStore = (Map<K, V>) HashMapFactory.make(t);
          backingStore.putAll(old);
        } else {
          backingStore.putAll(t);
          if (backingStore.size() > cutOff) {
            transferBackingStore();
          }
          return;
        }
      } else {
        backingStore.putAll(t);
      }
    }
  }

  /*
   * @see java.util.Map#clear()
   */
  @Override
  public void clear() {
    backingStore = null;
  }

  /*
   * @see java.util.Map#keySet()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Set<K> keySet() {
    return (Set<K>) ((backingStore == null) ? Collections.emptySet() : backingStore.keySet());
  }

  /*
   * @see java.util.Map#values()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Collection<V> values() {
    return (Collection<V>) ((backingStore == null) ? Collections.emptySet() : backingStore.values());
  }

  /**
   * @throws UnimplementedError if the backingStore implementation does
   */
  @Override
  @SuppressWarnings("unchecked")
  public Set<Map.Entry<K, V>> entrySet() {
    return (Set<Entry<K, V>>) ((backingStore == null) ? Collections.emptySet() : backingStore.entrySet());
  }
}
