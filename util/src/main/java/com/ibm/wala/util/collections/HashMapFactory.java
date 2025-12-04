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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * A debugging aid. When HashSetFactory.DEBUG is set, this class creates ParanoidHashMaps.
 * Otherwise, it returns {@link LinkedHashMap}
 */
public class HashMapFactory {

  /**
   * @return A ParanoidHashMap if DEBUG = true, a LinkedHashMap otherwise
   */
  public static <K, V> HashMap<K, V> make(int size) {
    if (HashSetFactory.DEBUG) {
      return new ParanoidHashMap<>(size);
    } else {
      return new LinkedHashMap<>(size);
    }
  }

  /**
   * @return A ParanoidHashMap if DEBUG = true, a LinkedHashMap otherwise
   */
  public static <K, V> HashMap<K, V> make() {
    if (HashSetFactory.DEBUG) {
      return new ParanoidHashMap<>();
    } else {
      return new LinkedHashMap<>();
    }
  }

  /**
   * @return A ParanoidHashMap if DEBUG = true, a LinkedHashMap otherwise
   */
  public static <K, V> HashMap<K, V> make(Map<? extends K, ? extends V> t) {
    if (t == null) {
      throw new IllegalArgumentException("null t");
    }
    if (HashSetFactory.DEBUG) {
      return new ParanoidHashMap<>(t);
    } else {
      return new LinkedHashMap<>(t);
    }
  }

  /**
   * Returns a {@code Collector} that accumulates the input elements into a new {@code HashMap} as
   * provided by {@link #make()}.
   *
   * @param <T> the type of the input elements
   * @param <K> the type of the result map's keys
   * @param <V> the type of the result map's values
   * @return a {@link Collector} that collects all the input elements into a {@link HashMap}
   */
  public static <T, K, V> @NotNull Collector<T, ?, HashMap<K, V>> toMap(
      @NotNull Function<? super T, ? extends K> keyMapper,
      @NotNull Function<? super T, ? extends V> valueMapper) {
    return Collectors.toMap(
        keyMapper,
        valueMapper,
        (left, right) -> {
          throw new IllegalArgumentException("cannot build `Map` with duplicate keys");
        },
        HashMapFactory::make);
  }
}
