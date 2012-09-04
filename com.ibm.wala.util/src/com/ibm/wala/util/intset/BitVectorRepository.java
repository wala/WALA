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

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.MapUtil;

/**
 * A repository for shared bit vectors as described by Heintze
 */
public class BitVectorRepository {

  private final static boolean STATS = false;

  private final static int STATS_WINDOW = 100;

  private static int queries = 0;

  private static int hits = 0;

  private final static int SUBSET_DELTA = 5;

  /**
   * A Mapping from Integer -> WeakHashMap
   */
  final private static Map<Object, WeakHashMap<BitVectorIntSet,Object>> buckets = HashMapFactory.make();

  /**
   * @param value
   * @return the BitVector in this repository which is the canonical shared
   *         subset representative of value; the result will have the same bits
   *         as value, except it may exclude up to SUBSET_DELTA bits.
   * @throws IllegalArgumentException  if value is null
   */
  public static synchronized BitVectorIntSet findOrCreateSharedSubset(BitVectorIntSet value) {
    if (value == null) {
      throw new IllegalArgumentException("value is null");
    }
    if (STATS) {
      queries++;
      if (queries % STATS_WINDOW == 0) {
        reportStats();
      }
    }
    int size = value.size();
    for (int i = size; i > size - SUBSET_DELTA; i--) {
      WeakHashMap<?, ?> m = buckets.get(Integer.valueOf(i));
      if (m != null) {
        for (Iterator<?> it = m.keySet().iterator(); it.hasNext();) {
          BitVectorIntSet bv = (BitVectorIntSet) it.next();
          if (bv.isSubset(value)) {
            // FOUND ONE!
            if (STATS) {
              hits++;
            }
            return bv;
          }
        }
      }
    }
    // didn't find one. create one.
    WeakHashMap<BitVectorIntSet, Object> m = MapUtil.findOrCreateWeakHashMap(buckets, new Integer(size));
    BitVectorIntSet bv = new BitVectorIntSet(value);
    m.put(bv, null);
    return bv;
  }

  /**
   * 
   */
  private static void reportStats() {
    double percent = 100.0 * hits / queries;
    System.err.println(("BitVectorRepository: queries " + queries + " hits " + percent));
    System.err.println(("                     entries " + countEntries()));
  }

  /**
   */
  private static int countEntries() {
    int result = 0;
    for (Iterator<WeakHashMap<BitVectorIntSet,Object>> it = buckets.values().iterator(); it.hasNext();) {
      WeakHashMap<?, ?> m = it.next();
      result += m.size();
    }
    return result;
  }

}