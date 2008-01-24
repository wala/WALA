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
package com.ibm.wala.cfg;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.ref.CacheReference;

/**
 *
 * A mapping from IMethod -> SoftReference -> ShrikeCFG
 * 
 * This doesn't work very well ... GCs don't do such a great job with
 * SoftReferences ... revamp it.
 * 
 * 
 * @author sfink
 */
public class CFGCache {

  /**
   * Help out the garbage collector: periodically "reset" this cache
   */
  final private static int RESET_INTERVAL = 10000;

  /**
   * A mapping from ShrikeCTMethodWrapper -> SoftReference -> IR
   */
  private HashMap<Object, Object> dictionary = HashMapFactory.make();

  /**
   * Count accesses between resets.
   */
  private int resetCount = 0;

  /**
   * The factory that actually creates new IR objects
   */
  private final IRFactory<IMethod> factory;

  public CFGCache(IRFactory<IMethod> factory) {
    this.factory = factory;
  }

  /**
   * @param m
   *          a "normal" (bytecode-based) method
   * @return an IR for m, built according to the specified options. null if m is
   *         abstract or native.
   * @throws IllegalArgumentException  if m is null
   */
  public synchronized ControlFlowGraph findOrCreate(IMethod m, Context C) {

    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isAbstract() || m.isNative()) {
      return null;
    }

    processResetLogic();

    Pair<IMethod,Context> p = Pair.make(m, C);
    Object ref = dictionary.get(p);
    if (ref == null || CacheReference.get(ref) == null) {
      ControlFlowGraph cfg = factory.makeCFG(m, C);
      ref = CacheReference.make(cfg);
      dictionary.put(p, ref);
      return cfg;
    } else {
      ControlFlowGraph cfg = (ControlFlowGraph) CacheReference.get(ref);
      return (cfg == null) ? findOrCreate(m, C) : cfg;
    }
  }

  private void processResetLogic() {
    resetCount++;
    if (resetCount == RESET_INTERVAL) {
      reset();
    }
  }

  /**
   * The existence of this is unfortunate.
   */
  public void wipe() {
    dictionary = HashMapFactory.make();
  }

  /**
   * clear out null refs
   */
  private void reset() {
    resetCount = 0;
    Map<Object, Object> oldDictionary = dictionary;
    dictionary = HashMapFactory.make();

    for (Iterator it = oldDictionary.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      Object key = e.getKey();
      Object val = e.getValue();
      if (CacheReference.get(val) != null) {
        dictionary.put(key, val);
      }
    }
  }

  /**
   * Invalidate cached information relating to a method
   * 
   * @param method
   */
  public void invalidate(IMethod method, Context C) {
    dictionary.remove(Pair.make(method, C));

  }
}
