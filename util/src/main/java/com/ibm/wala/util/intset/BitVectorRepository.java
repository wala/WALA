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

import com.ibm.wala.util.collections.HashMapFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A repository for shared bit vectors as described by Heintze.
 *
 * <p>Points-to sets that cross their sparse-to-dense threshold delegate their dense part here via
 * {@link #findOrCreateSharedSubset(BitVectorIntSet)}: the repository keeps one canonical
 * representative per distinct bit pattern and hands it back on later lookups, so equal sets share a
 * single backing array instead of each holding its own copy. Representatives are held by {@link
 * java.lang.ref.WeakReference} so the repository never pins memory; entries whose representative
 * has been collected are pruned opportunistically during lookups.
 *
 * <p>Representatives are bucketed by cardinality. Lookups scan the queried cardinality first, then
 * successively smaller cardinalities down to {@value #SUBSET_DELTA} elements below it. In the
 * exact-size bucket, a subset match of equal cardinality is necessarily bit-for-bit equality, so
 * each entry's cached {@link #fingerprint(int[])} rejects non-matching candidates with a single
 * {@code long} comparison before the word-by-word {@code BitVector.isSubset} confirmation runs.
 * Strictly smaller buckets keep the exact exhaustive subset scan; no scalar prefilter is sound
 * there because a strict subset's fingerprint need not match the queried set's.
 *
 * <p>The repository is a process-wide singleton guarded by {@code synchronized} on the lookup, and
 * lookups never mutate caller-visible state. The representative chosen for a given query is
 * deterministic (bucket insertion order), which keeps downstream iteration order stable.
 */
@SuppressWarnings("Java8MapApi")
public class BitVectorRepository {

  private static final boolean STATS = false;

  private static final int STATS_WINDOW = 100;

  private static int queries = 0;

  private static int hits = 0;

  private static final int SUBSET_DELTA = 5;

  private static final Map<Integer, List<Entry>> buckets = HashMapFactory.make();

  /**
   * One candidate representative. The {@link WeakReference} keeps the representative alive only as
   * long as the repository's clients do; once it has been cleared, the entry's {@link #fingerprint}
   * is stale and the entry itself is pruned the next time its bucket is scanned.
   */
  private record Entry(long fingerprint, WeakReference<BitVectorIntSet> reference) {}

  /**
   * Deterministic 64-bit fingerprint of a bit vector's words: a rotating-xor fold over the words,
   * mixed with the word count. Equal vectors always produce the same fingerprint; unequal vectors
   * may collide. Used only as a quick negative test ahead of {@link BitVector#isSubset}; collisions
   * are resolved by the exact subset check, so a colliding fingerprint can never cause a wrong
   * representative to be returned.
   */
  private static long fingerprint(int[] bits) {
    long fp = 0x9e3779b97f4a7c15L ^ (long) bits.length * 0x85ebca6bL;
    for (int bit : bits) {
      fp = (fp >>> 45) ^ (fp << 19) ^ ((bit & 0xffffffffL) * 0x9e3779b97f4a7c15L);
    }
    return fp;
  }

  /**
   * @return the BitVector in this repository which is the canonical shared subset representative of
   *     value; the result will have the same bits as value, except it may exclude up to
   *     SUBSET_DELTA bits.
   * @throws IllegalArgumentException if value is null
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
    long fingerprint = fingerprint(value.getBitVector().bits);
    // All representatives in the exact-size bucket have the same cardinality as value, and a
    // same-cardinality subset of value is necessarily equal to value. Equal contents imply equal
    // fingerprints, so a differing fingerprint soundly excludes a candidate without running the
    // word-by-word scan; only fingerprint collisions (and the true match) pay for isSubset.
    List<Entry> m = buckets.get(size);
    if (m != null) {
      for (int idx = 0; idx < m.size(); ) {
        Entry e = m.get(idx);
        BitVectorIntSet bv = e.reference.get();
        if (bv == null) {
          m.remove(idx);
          continue;
        }
        if (e.fingerprint == fingerprint && bv.isSubset(value)) {
          if (STATS) {
            hits++;
          }
          return bv;
        }
        idx++;
      }
    }
    // Strictly smaller representatives can still be a subset of value. No scalar prefilter is
    // sound here (a strict subset's fingerprint need not match value's), so these buckets keep the
    // exact exhaustive subset scan; in practice they hold few candidates.
    for (int i = size - 1; i > size - SUBSET_DELTA; i--) {
      List<Entry> list = buckets.get(i);
      if (list != null) {
        for (int idx = 0; idx < list.size(); ) {
          Entry e = list.get(idx);
          BitVectorIntSet bv = e.reference.get();
          if (bv == null) {
            // remove the weak reference to avoid leaks
            list.remove(idx);
            continue;
          }
          if (bv.isSubset(value)) {
            // FOUND ONE!
            if (STATS) {
              hits++;
            }
            return bv;
          }
          idx++;
        }
      }
    }
    // didn't find one. create one.
    if (m == null) {
      m = new ArrayList<>();
      buckets.put(size, m);
    }
    BitVectorIntSet bv = new BitVectorIntSet(value);
    m.add(new Entry(fingerprint, new WeakReference<>(bv)));
    return bv;
  }

  private static void reportStats() {
    double percent = 100.0 * hits / queries;
    System.err.println(("BitVectorRepository: queries " + queries + " hits " + percent));
    System.err.println(("                     entries " + countEntries()));
  }

  private static int countEntries() {
    int result = 0;
    for (List<Entry> l : buckets.values()) {
      // don't worry about cleared WeakReferences; count will be rough
      result += l.size();
    }
    return result;
  }
}
