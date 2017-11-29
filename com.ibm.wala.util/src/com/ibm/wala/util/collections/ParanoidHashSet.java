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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.debug.UnimplementedError;

/**
 * a debugging aid. This implementation complains if you stick an object in here which appears to use System.identityHashCode(), or
 * if it detects more than BAD_HC collisions in the Set (possibly indicated a bad hash function)
 */
public class ParanoidHashSet<T> extends LinkedHashSet<T> {
  public static final long serialVersionUID = 30919839181133333L;

  /**
   * A mapping from Integer (hashcode) -&gt; Set of objects
   */
  private final Map<Integer, Set<T>> hcFreq;

  private int nAdded = 0;

  /**
   * If a hash set contains more than this number of items with the same hash code, complain.
   */
  private final int BAD_HC = 3;

  /**
   * @param s
   * @throws NullPointerException if s is null
   */
  public ParanoidHashSet(Collection<T> s) throws NullPointerException {
    super(s.size());
    hcFreq = HashMapFactory.make(s.size());
    for (T t : s) {
      add(t);
    }
  }

  /**
   * 
   */
  public ParanoidHashSet() {
    super();
    hcFreq = HashMapFactory.make();
  }

  public ParanoidHashSet(int size) {
    super(size);
    hcFreq = HashMapFactory.make(size);
  }

  /**
   * @see java.util.Collection#add(java.lang.Object)
   * @throws UnimplementedError if there's a bad hash code problem
   */
  @Override
  public boolean add(T arg0) {
    if (arg0 == null) {
      throw new IllegalArgumentException("arg0 is null");
    }

    ParanoidHashMap.assertOverridesHashCode(arg0);
    boolean result = super.add(arg0);
    if (result) {
      nAdded++;
      int hc = arg0.hashCode();
      Set<T> s = hcFreq.get(hc);
      if (s == null) {
        HashSet<T> h = new LinkedHashSet<>(1);
        h.add(arg0);
        hcFreq.put(hc, h);
      } else {
        if (s.size() == BAD_HC) {
          for (T t : s) {
            Object o = t;
            System.err.println(o + " " + o.hashCode());
          }
          assert false : "bad hc " + arg0.getClass() + " " + arg0;
        } else {
          s.add(arg0);
        }
      }
    }
    return result;
  }
}
