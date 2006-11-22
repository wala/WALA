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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * a debugging aid. This implementation complains if you stick an object in here
 * which appears to use System.identityHashCode(), or if it detects more than
 * BAD_HC collisions in the Set (possibly indicated a bad hash function)
 * 
 * @author sfink
 */
public class ParanoidHashSet<T> extends HashSet<T> {
  public static final long serialVersionUID = 30919839181133333L;

  /**
   * A mapping from Integer (hashcode) -> Set of objects
   */
  private final Map<Integer, Set<T>> hcFreq;

  private int nAdded = 0;

  /**
   * If a hash set contains more than this number of items with the same hash
   * code, complain.
   */
  private final int BAD_HC = 3;

  /**
   * @param s
   */
  public ParanoidHashSet(Set<T> s) {
    super(s.size());
    hcFreq = new HashMap<Integer, Set<T>>(s.size());
    for (Iterator<T> it = s.iterator(); it.hasNext();) {
      add(it.next());
    }
  }

  /**
   * 
   */
  public ParanoidHashSet() {
    super();
    hcFreq = new HashMap<Integer, Set<T>>();
  }

  /**
   * @param size
   */
  public ParanoidHashSet(int size) {
    super(size);
    hcFreq = new HashMap<Integer, Set<T>>(size);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Collection#add(java.lang.Object)
   */
  public boolean add(T arg0) {
    if (arg0.hashCode() == System.identityHashCode(arg0)) {
      Assertions._assert(false, arg0.getClass().toString());
    }
    boolean result = super.add(arg0);
    if (result) {
      nAdded++;
      int hc = arg0.hashCode();
      Set<T> s = hcFreq.get(new Integer(hc));
      if (s == null) {
        HashSet<T> h = new HashSet<T>(1);
        h.add(arg0);
        hcFreq.put(new Integer(hc), h);
      } else {
        if (s.size() == BAD_HC) {
          for (Iterator<T> it = s.iterator(); it.hasNext();) {
            Object o = it.next();
            System.err.println(o + " " + o.hashCode());
          }
          Assertions._assert(false, "bad hc " + arg0.getClass() + " " + arg0);
        } else {
          s.add(arg0);
        }
      }
    }
    return result;
  }

}