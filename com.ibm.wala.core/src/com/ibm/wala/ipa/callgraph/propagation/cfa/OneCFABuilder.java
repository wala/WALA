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
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.ReflectionSpecification;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ClassBasedInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 
 * 1-CFA Call graph builder
 * 
 * @author sfink
 */
public class OneCFABuilder extends SSAPropagationCallGraphBuilder {

  public static OneCFABuilder make(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, ReflectionSpecification reflect)
      throws IllegalArgumentException {
    if (options == null) {
      throw new IllegalArgumentException("options == null");
    }
    return new OneCFABuilder(cha, options, cache, appContextSelector, appContextInterpreter, reflect);
  }

  private OneCFABuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache, ContextSelector appContextSelector,
      SSAContextInterpreter appContextInterpreter, ReflectionSpecification reflect) {

    super(cha, options, cache, new DefaultPointerKeyFactory());

    setInstanceKeys(new ClassBasedInstanceKeys(options, cha));

    ContextSelector def = new DefaultContextSelector(cha, options.getMethodTargetSelector());
    ContextSelector contextSelector = new DelegatingContextSelector(appContextSelector, def);
    contextSelector = new OneLevelContextSelector(contextSelector);
    setContextSelector(contextSelector);

    SSAContextInterpreter defI = new DefaultSSAInterpreter(options, getAnalysisCache());
    defI = new DelegatingSSAContextInterpreter(new FactoryBypassInterpreter(options, getAnalysisCache(), reflect), defI);
    SSAContextInterpreter contextInterpreter = new DelegatingSSAContextInterpreter(appContextInterpreter, defI);
    setContextInterpreter(contextInterpreter);
  }

}
