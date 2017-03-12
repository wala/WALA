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
import com.ibm.wala.ssa.AuxiliaryCache;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSACache;
import com.ibm.wala.ssa.SSAOptions;

public class AnalysisCacheImpl extends AnalysisCache {

  public AnalysisCacheImpl(IRFactory<IMethod> irFactory, SSAOptions ssaOptions) {
    super(irFactory, ssaOptions, new SSACache(irFactory, new AuxiliaryCache(), new AuxiliaryCache()));
  }
  
  public AnalysisCacheImpl(IRFactory<IMethod> irFactory) {
    this(irFactory, new AnalysisOptions().getSSAOptions());
  }
  
  public AnalysisCacheImpl() {
    this(new DefaultIRFactory());
  }

}
