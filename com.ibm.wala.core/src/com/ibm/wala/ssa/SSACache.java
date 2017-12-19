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

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;

/**
 * A mapping from IMethod -&gt; SSAOptions -&gt; SoftReference -&gt; Something
 * 
 * This doesn't work very well ... GCs don't do such a great job with SoftReferences ... revamp it.
 */
public class SSACache {

  /**
   * used for debugging
   */
  private static final boolean DISABLE = false;

  /**
   * The factory that actually creates new IR objects
   */
  private final IRFactory<IMethod> factory;

  /**
   * A cache of SSA IRs
   */
  final private IAuxiliaryCache irCache;

  /**
   * A cache of DefUse information
   */
  final private IAuxiliaryCache duCache;

  /**
   * @param factory a factory for creating IRs
   */
  public SSACache(IRFactory<IMethod> factory, IAuxiliaryCache irCache, IAuxiliaryCache duCache) {
    this.factory = factory;
    this.irCache = irCache;
    this.duCache = duCache;
  }

  /**
   * @param m a "normal" (bytecode-based) method
   * @param options options governing ssa construction
   * @return an IR for m, built according to the specified options. null if m is abstract or native.
   * @throws IllegalArgumentException if m is null
   */
  public synchronized IR findOrCreateIR(final IMethod m, Context c, final SSAOptions options) {

    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isAbstract() || m.isNative()) {
      return null;
    }

    if (factory.contextIsIrrelevant(m)) {
      c = Everywhere.EVERYWHERE;
    }

    if (DISABLE) {
      return factory.makeIR(m, c, options);
    }

    IR ir = (IR) irCache.find(m, c, options);
    if (ir == null) {
      ir = factory.makeIR(m, c, options);
      irCache.cache(m, c, options, ir);
    }
    return ir;
  }

  /**
   * @param m a method
   * @param options options governing ssa construction
   * @return DefUse information for m, built according to the specified options. null if unavailable
   * @throws IllegalArgumentException if m is null
   */
  public synchronized DefUse findOrCreateDU(IMethod m, Context c, SSAOptions options) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isAbstract() || m.isNative()) {
      return null;
    }
    if (factory.contextIsIrrelevant(m)) {
      c = Everywhere.EVERYWHERE;
    }

    DefUse du = (DefUse) duCache.find(m, c, options);
    if (du == null) {
      IR ir = findOrCreateIR(m, c, options);
      du = new DefUse(ir);
      duCache.cache(m, c, options, du);
    }
    return du;
  }

  /**
   * @return {@link DefUse} information for m, built according to the specified options. null if unavailable
   * @throws IllegalArgumentException if ir is null
   */
  public synchronized DefUse findOrCreateDU(IR ir, Context C) {
    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    DefUse du = (DefUse) duCache.find(ir.getMethod(), C, ir.getOptions());
    if (du == null) {
      du = new DefUse(ir);
      duCache.cache(ir.getMethod(), C, ir.getOptions(), du);
    }
    return du;
  }

  /**
   * The existence of this is unfortunate.
   */
  public void wipe() {
    irCache.wipe();
    duCache.wipe();
  }

  /**
   * Invalidate the cached IR for a &lt;method,context&gt; pair
   */
  public void invalidateIR(IMethod method, Context c) {
    irCache.invalidate(method, c);
  }

  /**
   * Invalidate the cached {@link DefUse} for a &lt;method,context&gt; pair
   */
  public void invalidateDU(IMethod method, Context c) {
    duCache.invalidate(method, c);
  }

  /**
   * Invalidate all cached information for a &lt;method,context&gt; pair
   */
  public void invalidate(IMethod method, Context c) {
    invalidateIR(method, c);
    invalidateDU(method, c);
  }
}
