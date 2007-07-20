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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.analysis.reflection.FactoryBypassInterpreter;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ReflectionSpecification;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 
 * Common utilities for CFA-style call graph builders.
 * 
 * @author sfink
 * 
 */
public abstract class CFABuilder extends SSAPropagationCallGraphBuilder {

  /**
   * @throws NullPointerException
   *             if options is null
   */
  public CFABuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) throws NullPointerException {
    super(cha, options, cache, new CFAPointerKeys());
  }

  public SSAContextInterpreter makeDefaultContextInterpreters(SSAContextInterpreter appContextInterpreter, AnalysisOptions options, 
      ReflectionSpecification reflect) {
    SSAContextInterpreter c = new DefaultSSAInterpreter(options, getAnalysisCache());
    c = new DelegatingSSAContextInterpreter(new FactoryBypassInterpreter(options, getAnalysisCache(), reflect), c);
    return new DelegatingSSAContextInterpreter(appContextInterpreter, c);
  }

}
