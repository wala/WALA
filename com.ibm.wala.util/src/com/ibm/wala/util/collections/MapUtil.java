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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * utilities for managing {@link Map}s
 */
public class MapUtil {
  /**
   * @param M a mapping from Object -&gt; Set
   * @param key
   * @return the Set corresponding to key in M; create one if needed
   * @throws IllegalArgumentException if M is null
   * @throws ClassCastException if the key is of an inappropriate type for this map (optional)
   * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
   */
  public static <K, T> Set<T> findOrCreateSet(Map<K, Set<T>> M, K key) {
    if (M == null) {
      throw new IllegalArgumentException("M is null");
    }
    Set<T> result = M.get(key);
    if (result == null) {
      result = HashSetFactory.make(2);
      M.put(key, result);
    }
    return result;
  }

  /**
   * @throws ClassCastException if the key is of an inappropriate type for this map (optional)
   * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
   */
  public static <K> MutableIntSet findOrCreateMutableIntSet(Map<K, MutableIntSet> M, K key) {
    if (M == null) {
      throw new IllegalArgumentException("M is null");
    }
    MutableIntSet mis = M.get(key);
    if (mis == null) {
      mis = MutableSparseIntSet.makeEmpty();
      M.put(key, mis);
    }
    return mis;
  }

  /**
   * @return the Collection corresponding to key in M; create one if needed
   * @throws ClassCastException if the key is of an inappropriate type for this map (optional)
   * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
   */
  public static <K, T> Collection<T> findOrCreateCollection(Map<K, Collection<T>> M, K key) {
    if (M == null) {
      throw new IllegalArgumentException("M is null");
    }
    Collection<T> result = M.get(key);
    if (result == null) {
      result = HashSetFactory.make(2);
      M.put(key, result);
    }
    return result;
  }

  /**
   * @return the Set corresponding to key in M; create one if needed
   * @throws IllegalArgumentException if M is null
   * @throws ClassCastException if the key is of an inappropriate type for this map (optional)
   * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
   */
  public static <K, T> List<T> findOrCreateList(Map<K, List<T>> M, K key) {
    if (M == null) {
      throw new IllegalArgumentException("M is null");
    }
    List<T> result = M.get(key);
    if (result == null) {
      result = new ArrayList<>();
      M.put(key, result);
    }
    return result;
  }

  /**
   * @param M a mapping from Object -&gt; Map
   * @param key
   * @return the Map corresponding to key in M; create one if needed
   * @throws IllegalArgumentException if M is null
   * @throws ClassCastException if the key is of an inappropriate type for this map (optional)
   * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
   */
  public static <K, K2, V> Map<K2, V> findOrCreateMap(Map<K, Map<K2, V>> M, K key) {
    if (M == null) {
      throw new IllegalArgumentException("M is null");
    }
    Map<K2, V> result = M.get(key);
    if (result == null) {
      result = HashMapFactory.make(2);
      M.put(key, result);
    }
    return result;
  }

  /**
   * @throws ClassCastException if the key is of an inappropriate type for this map (optional)
   * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
   */
  public static <K, V> V findOrCreateValue(Map<K, V> M, K key, Factory<V> factory) {
    if (M == null) {
      throw new IllegalArgumentException("M is null");
    }
    V result = M.get(key);
    if (result == null) {
      result = factory.make();
      M.put(key, result);
    }
    return result;
  }

  /**
   * @param M a mapping from Object -&gt; WeakHashMap
   * @param key
   * @return the WeakHashMap corresponding to key in M; create one if needed
   * @throws IllegalArgumentException if M is null
   * @throws ClassCastException if the key is of an inappropriate type for this map (optional)
   * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
   */
  public static <K, V> WeakHashMap<K, V> findOrCreateWeakHashMap(Map<Object, WeakHashMap<K, V>> M, Object key) {
    if (M == null) {
      throw new IllegalArgumentException("M is null");
    }
    WeakHashMap<K, V> result = M.get(key);
    if (result == null) {
      result = new WeakHashMap<>(2);
      M.put(key, result);
    }
    return result;
  }

  /**
   * @param m a map from key -&gt; {@link Set}&lt;value&gt;
   * @return inverted map, value -&gt; {@link Set}&lt;key&gt;
   * @throws IllegalArgumentException if m is null
   */
  public static <K, V> Map<V, Set<K>> inverseMap(Map<K, Set<V>> m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    Map<V, Set<K>> result = HashMapFactory.make(m.size());
    for (Map.Entry<K, Set<V>> E : m.entrySet()) {
      K key = E.getKey();
      Set<V> values = E.getValue();
      for (V v : values) {
        Set<K> s = findOrCreateSet(result, v);
        s.add(key);
      }
    }
    return result;
  }

  /**
   * invert an input map that is one-to-one (i.e., it does not map two different keys to the same value)
   * 
   * @throws IllegalArgumentException if m is null
   * @throws IllegalArgumentException if m is not one-to-one
   */
  public static <K, V> Map<V, K> invertOneToOneMap(Map<K, V> m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    Map<V, K> result = HashMapFactory.make(m.size());
    for (Map.Entry<K, V> entry : m.entrySet()) {
      K key = entry.getKey();
      V val = entry.getValue();
      if (result.containsKey(val)) {
        throw new IllegalArgumentException("input map not one-to-one");
      }
      result.put(val, key);
    }
    return result;
  }

  public static <K, V> Map<Set<K>, V> groupKeysByValue(Map<K, V> m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    Map<Set<K>, V> result = HashMapFactory.make();
    Map<V, Set<K>> valueToKeys = HashMapFactory.make();
    for (Map.Entry<K, V> E : m.entrySet()) {
      K key = E.getKey();
      V value = E.getValue();
      findOrCreateSet(valueToKeys, value).add(key);
    }
    for (Map.Entry<V, Set<K>> E : valueToKeys.entrySet()) {
      V value = E.getKey();
      Set<K> keys = E.getValue();
      result.put(keys, value);
    }
    return result;
  }
}
