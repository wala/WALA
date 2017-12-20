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
package com.ibm.wala.ssa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.ref.CacheReference;

/**
 * A cache for auxiliary information based on an SSA representation
 * 
 * A mapping from (IMethod,Context) -&gt; SSAOptions -&gt; SoftReference -&gt; something
 * 
 * This doesn't work very well ... GCs don't do such a great job with SoftReferences ... revamp it.
 */
public class AuxiliaryCache implements IAuxiliaryCache {

  /**
   * A mapping from IMethod -&gt; SSAOptions -&gt; SoftReference -&gt; IR
   */
  private HashMap<Pair<IMethod, Context>, Map<SSAOptions, Object>> dictionary = HashMapFactory.make();

  /**
   * Help out the garbage collector: clear this cache when the number of items is &gt; RESET_THRESHOLD
   */
  final private static int RESET_THRESHOLD = 2000;

  /**
   * number of items cached here.
   */
  private int nItems = 0;

  /* 
   * @see com.ibm.wala.ssa.IAuxiliaryCache#wipe()
   */
  @Override
  public synchronized void wipe() {
    dictionary = HashMapFactory.make();
    nItems = 0;
  }

  /**
   * clear out things from which no IR is reachable
   */
  private void reset() {
    Map<Pair<IMethod, Context>, Map<SSAOptions, Object>> oldDictionary = dictionary;
    dictionary = HashMapFactory.make();
    nItems = 0;

    for (Entry<Pair<IMethod, Context>, Map<SSAOptions, Object>> e : oldDictionary.entrySet()) {
   Map<SSAOptions, Object> m = e.getValue();
   HashSet<Object> toRemove = HashSetFactory.make();
   for (Entry<SSAOptions, Object> e2 : m.entrySet()) {
    Object key = e2.getKey();
    Object val = e2.getValue();
    if (CacheReference.get(val) == null) {
      toRemove.add(key);
    }
   }
   for (Object object : toRemove) {
    m.remove(object);
   }
   if (m.size() > 0) {
    dictionary.put(e.getKey(), m);
   }
  }
  }

  /* 
   * @see com.ibm.wala.ssa.IAuxiliaryCache#find(com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.callgraph.Context, com.ibm.wala.ssa.SSAOptions)
   */
  @Override
  public synchronized Object find(IMethod m, Context c, SSAOptions options) {
    // methodMap: SSAOptions -> SoftReference
    Pair<IMethod, Context> p = Pair.make(m, c);
    Map methodMap = MapUtil.findOrCreateMap(dictionary, p);
    Object ref = methodMap.get(options);
    if (ref == null || CacheReference.get(ref) == null) {
      return null;
    } else {
      return CacheReference.get(ref);
    }
  }

  /* 
   * @see com.ibm.wala.ssa.IAuxiliaryCache#cache(com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.callgraph.Context, com.ibm.wala.ssa.SSAOptions, java.lang.Object)
   */
  @Override
  public synchronized void cache(IMethod m, Context c, SSAOptions options, Object aux) {
    nItems++;

    if (nItems > RESET_THRESHOLD) {
      reset();
    }
    Pair<IMethod, Context> p = Pair.make(m, c);
    // methodMap: SSAOptions -> SoftReference
    Map<SSAOptions, Object> methodMap = MapUtil.findOrCreateMap(dictionary, p);
    Object ref = CacheReference.make(aux);
    methodMap.put(options, ref);
  }

  /* 
   * @see com.ibm.wala.ssa.IAuxiliaryCache#invalidate(com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.callgraph.Context)
   */
  @Override
  public void invalidate(IMethod method, Context c) {
    dictionary.remove(Pair.make(method, c));
  }
}
