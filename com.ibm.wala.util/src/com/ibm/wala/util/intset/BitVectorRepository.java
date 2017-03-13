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

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.ibm.wala.util.collections.HashMapFactory;

/**
 * A repository for shared bit vectors as described by Heintze
 */
public class BitVectorRepository {

  private final static boolean STATS = false;

  private final static int STATS_WINDOW = 100;

  private static int queries = 0;

  private static int hits = 0;

  private final static int SUBSET_DELTA = 5;

  final private static Map<Integer, LinkedList<WeakReference<BitVectorIntSet>>> buckets = HashMapFactory.make();

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
      LinkedList<WeakReference<BitVectorIntSet>> m = buckets.get(Integer.valueOf(i));
      if (m != null) {
        Iterator<WeakReference<BitVectorIntSet>> it = m.iterator();
        while (it.hasNext()) {
          WeakReference<BitVectorIntSet> wr = it.next();
          BitVectorIntSet bv = wr.get();
          if (bv != null) {
            if (bv.isSubset(value)) {
              // FOUND ONE!
              if (STATS) {
                hits++;
              }
              return bv;            
            }
          } else {
            // remove the weak reference to avoid leaks
            it.remove();
          }
        }
      }
    }
    // didn't find one. create one.
    LinkedList<WeakReference<BitVectorIntSet>> m = buckets.get(size);
    if (m == null) {
      m = new LinkedList<>();
      buckets.put(size, m);
    }
    BitVectorIntSet bv = new BitVectorIntSet(value);
    m.add(new WeakReference<>(bv));
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
    for (LinkedList<WeakReference<BitVectorIntSet>> l : buckets.values()) {
      // don't worry about cleared WeakReferences; count will be rough
      result += l.size();
    }
    return result;
  }

}
