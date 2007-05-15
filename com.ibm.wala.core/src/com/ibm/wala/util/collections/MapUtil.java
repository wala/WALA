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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 
 * utilities for managing Maps
 * 
 * @author sfink
 */
public class MapUtil {
  /**
   * @param M
   *          a mapping from Object -> Set
   * @param key
   * @return the Set corresponding to key in M; create one if needed
   */
  public static <K, T> Set<T> findOrCreateSet(Map<K, Set<T>> M, K key) {
    Set<T> result = M.get(key);
    if (result == null) {
      result = HashSetFactory.make(2);
      M.put(key, result);
    }
    return result;
  }

  /**
   * @param M
   *          a mapping from Object -> Map
   * @param key
   * @return the Map corresponding to key in M; create one if needed
   */
  public static <K, K2, V> Map<K2, V> findOrCreateMap(Map<K, Map<K2, V>> M, K key) {
    Map<K2, V> result = M.get(key);
    if (result == null) {
      result = HashMapFactory.make(2);
      M.put(key, result);
    }
    return result;
  }

  /**
   * @param M
   *          a mapping from Object -> WeakHashMap
   * @param key
   * @return the WeakHashMap corresponding to key in M; create one if needed
   * @throws IllegalArgumentException
   *           if M is null
   */
  public static <K, V> WeakHashMap<K, V> findOrCreateWeakHashMap(Map<Object, WeakHashMap<K, V>> M, Object key) {
    if (M == null) {
      throw new IllegalArgumentException("M is null");
    }
    WeakHashMap<K, V> result = M.get(key);
    if (result == null) {
      result = new WeakHashMap<K, V>(2);
      M.put(key, result);
    }
    return result;
  }

  /**
   * @param m
   *          a map from key -> Set <value>
   * @return inverted map, value -> Set <key>
   * @throws IllegalArgumentException  if m is null
   */
  public static <K, V> Map<V, Set<K>> inverseMap(Map<K, Set<V>> m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    Map<V, Set<K>> result = HashMapFactory.make(m.size());
    for (Iterator<Map.Entry<K, Set<V>>> it = m.entrySet().iterator(); it.hasNext();) {
      Map.Entry<K, Set<V>> E = it.next();
      K key = E.getKey();
      Set<V> values = E.getValue();
      for (Iterator<V> it2 = values.iterator(); it2.hasNext();) {
        V v = it2.next();
        Set<K> s = findOrCreateSet(result, v);
        s.add(key);
      }
    }
    return result;
  }

  public static <K, V> Map<Set<K>, V> groupKeysByValue(Map<K, V> m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    Map<Set<K>, V> result = HashMapFactory.make();
    Map<V, Set<K>> valueToKeys = HashMapFactory.make();
    for (Iterator<Map.Entry<K, V>> it = m.entrySet().iterator(); it.hasNext();) {
      Map.Entry<K, V> E = it.next();
      K key = E.getKey();
      V value = E.getValue();
      findOrCreateSet(valueToKeys, value).add(key);
    }
    for (Iterator<Map.Entry<V, Set<K>>> it = valueToKeys.entrySet().iterator(); it.hasNext();) {
      Map.Entry<V, Set<K>> E = it.next();
      V value = E.getKey();
      Set<K> keys = E.getValue();
      result.put(keys, value);
    }
    return result;
  }
}
