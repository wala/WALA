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

import com.ibm.wala.cfg.CFGCache;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSACache;
import com.ibm.wala.util.ReferenceCleanser;

/**
 * A place to hold onto caches of various analysis artifacts.
 * 
 * Someday this should maybe go away?
 * 
 * @author sjfink
 */
public class AnalysisCache {
  private final IRFactory irFactory;

  private final SSACache ssaCache;

  private final CFGCache cfgCache;

  public AnalysisCache(IRFactory irFactory) {
    super();
    this.irFactory = irFactory;
    this.ssaCache = new SSACache(irFactory);
    this.cfgCache = new CFGCache(irFactory);
    ReferenceCleanser.registerCache(this);
  }

  public AnalysisCache() {
    this(new DefaultIRFactory());
  }

  public void invalidate(IMethod method, Context C) {
    ssaCache.invalidate(method, C);
    cfgCache.invalidate(method, C);
  }

  public SSACache getSSACache() {
    return ssaCache;
  }

  public CFGCache getCFGCache() {
    return cfgCache;
  }

  public IRFactory getIRFactory() {
    return irFactory;
  }

}
