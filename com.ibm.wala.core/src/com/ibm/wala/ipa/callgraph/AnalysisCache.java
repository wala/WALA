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
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSACache;
import com.ibm.wala.util.ref.ReferenceCleanser;

/**
 * A place to hold onto caches of various analysis artifacts.
 * 
 * Someday this should maybe go away?
 */
public class AnalysisCache {
  private final IRFactory<IMethod> irFactory;

  private final SSACache ssaCache;

  public AnalysisCache(IRFactory<IMethod> irFactory) {
    super();
    this.irFactory = irFactory;
    this.ssaCache = new SSACache(irFactory);
    ReferenceCleanser.registerCache(this);
  }

  public AnalysisCache() {
    this(new DefaultIRFactory());
  }

  public void invalidate(IMethod method, Context C) {
    ssaCache.invalidate(method, C);
  }

  public SSACache getSSACache() {
    return ssaCache;
  }

  public IRFactory<IMethod> getIRFactory() {
    return irFactory;
  }
}
