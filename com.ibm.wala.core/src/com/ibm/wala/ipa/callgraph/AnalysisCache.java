/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSACache;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.ref.ReferenceCleanser;

/**
 * A place to hold onto caches of various analysis artifacts.
 * 
 * Someday this should maybe go away?
 */
public class AnalysisCache implements IAnalysisCacheView {
  private final IRFactory<IMethod> irFactory;

  private final SSACache ssaCache;

  private final SSAOptions ssaOptions;
  
  public AnalysisCache(IRFactory<IMethod> irFactory, SSAOptions ssaOptions, SSACache cache) {
    super();
    this.ssaOptions = ssaOptions;
    this.irFactory = irFactory;
    this.ssaCache = cache;
    ReferenceCleanser.registerCache(this);
  }

  /* 
   * @see com.ibm.wala.ipa.callgraph.IAnalysisCacheView#invalidate(com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.callgraph.Context)
   */
  @Override
  public void invalidate(IMethod method, Context C) {
    ssaCache.invalidate(method, C);
  }

  public SSACache getSSACache() {
    return ssaCache;
  }

  public SSAOptions getSSAOptions() {
    return ssaOptions;
  }

  /* 
   * @see com.ibm.wala.ipa.callgraph.IAnalysisCacheView#getIRFactory()
   */
  @Override
  public IRFactory<IMethod> getIRFactory() {
    return irFactory;
  }

  /* 
   * @see com.ibm.wala.ipa.callgraph.IAnalysisCacheView#getIR(com.ibm.wala.classLoader.IMethod)
   */
  @Override
  public IR getIR(IMethod method, Context context) {
    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    return ssaCache.findOrCreateIR(method, context, ssaOptions);
  }

  @Override
  public IR getIR(IMethod m) {
    return getIR(m, Everywhere.EVERYWHERE);
  }
  
  /* 
   * @see com.ibm.wala.ipa.callgraph.IAnalysisCacheView#getDefUse(com.ibm.wala.ssa.IR)
   */
  @Override
  public DefUse getDefUse(IR ir) {
    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    return ssaCache.findOrCreateDU(ir, Everywhere.EVERYWHERE);
  }
}
