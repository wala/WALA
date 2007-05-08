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
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * A mapping from IMethod -> SSAOptions -> SoftReference -> IR
 * 
 * This doesn't work very well ... GCs don't do such a great job with
 * SoftReferences ... revamp it.
 * 
 * @author sfink
 */
public class SSACache {

  /**
   * used for debugging
   */
  private static final boolean DISABLE = false;

  /**
   * The factory that actually creates new IR objects
   */
  private final IRFactory factory;

  /**
   * A cache of SSA IRs
   */
  private AuxiliaryCache irCache = new AuxiliaryCache();

  /**
   * A cache of DefUse information
   */
  private AuxiliaryCache duCache = new AuxiliaryCache();

  /**
   * @param factory
   */
  public SSACache(IRFactory factory) {
    this.factory = factory;
  }

  /**
   * @param m
   *          a "normal" (bytecode-based) method
   * @param options
   *          options governing ssa construction
   * @param warnings
   *          an option to track analysis warnings
   * @return an IR for m, built according to the specified options. null if m is
   *         abstract or native.
   */
  public synchronized IR findOrCreateIR(IMethod m, Context C, ClassHierarchy cha, SSAOptions options, WarningSet warnings) {

    if (m.isAbstract() || m.isNative()) {
      return null;
    }

    if (DISABLE) {
      return factory.makeIR(m, C, cha, options, warnings);
    }

    IR ir = (IR)irCache.find(m,C, options);
    if (ir == null) {
      ir = factory.makeIR(m, C, cha, options, warnings);
      irCache.cache(m,C, options,ir);
    }
    return ir;
  }

  /**
   * @param m
   *          a method
   * @param options
   *          options governing ssa construction
   * @param warnings
   *          an option to track analysis warnings
   * @return DefUse information for m, built according to the specified options.  null if unavailable
   */
  public synchronized DefUse findOrCreateDU(IMethod m, Context C, ClassHierarchy cha, SSAOptions options, WarningSet warnings) {
   
    if (m.isAbstract() || m.isNative()) {
      return null;
    }
    
    DefUse du = (DefUse)duCache.find(m,C,options);
    if (du == null) {
      IR ir = findOrCreateIR(m,C,cha,options,warnings);
      du = new DefUse(ir);
      duCache.cache(m,C,options,du);
    }
    return du;
  }
  
  /**
   * @return DefUse information for m, built according to the specified options.  null if unavailable
   * @throws IllegalArgumentException  if ir is null
   */
  public synchronized DefUse findOrCreateDU(IR ir, Context C) {
    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    DefUse du = (DefUse)duCache.find(ir.getMethod(),C,ir.getOptions());
    if (du == null) {
      du = new DefUse(ir);
      duCache.cache(ir.getMethod(),C,ir.getOptions(),du);
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

  public void invalidateIR(IMethod method, Context C) {
    irCache.invalidate(method, C);
  }

  public void invalidateDU(IMethod method, Context C) {
    duCache.invalidate(method, C);
  }

  public void invalidate(IMethod method, Context C) {
    invalidateIR(method, C);
    invalidateDU(method, C);
  }

}
