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
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 0-1-CFA Call graph builder which analyzes calls to "container methods" in a
 * context which is defined by the receiver instance.
 * 
 * @author sfink
 */
public class ZeroOneContainerCFABuilder extends CFABuilder {

  /**
   * @param cha
   *            governing class hierarchy
   * @param options
   *            call graph construction options
   * @param appContextSelector
   *            application-specific logic to choose contexts
   * @param appContextInterpreter
   *            application-specific logic to interpret a method in context
   * @param reflect
   *            reflection specification
   * @throws IllegalArgumentException
   *             if options is null
   */
  public ZeroOneContainerCFABuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, ReflectionSpecification reflect) {

    super(cha, options, cache);
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }

    ContextSelector def = new DefaultContextSelector(cha, options.getMethodTargetSelector());
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);

    SSAContextInterpreter c = new DefaultSSAInterpreter(options, cache);
    c = new DelegatingSSAContextInterpreter(new FactoryBypassInterpreter(options, getAnalysisCache(), reflect), c);
    SSAContextInterpreter contextInterpreter = new DelegatingSSAContextInterpreter(appContextInterpreter, c);
    setContextInterpreter(contextInterpreter);

    ZeroXInstanceKeys zik = makeInstanceKeys(cha, options, contextInterpreter);
    setInstanceKeys(zik);

    ContextSelector CCS = makeContainerContextSelector(cha, (ZeroXInstanceKeys) getInstanceKeys());
    DelegatingContextSelector DCS = new DelegatingContextSelector(CCS, contextSelector);
    setContextSelector(DCS);
  }

  protected ZeroXInstanceKeys makeInstanceKeys(IClassHierarchy cha, AnalysisOptions options,
      SSAContextInterpreter contextInterpreter) {
    ZeroXInstanceKeys zik = new ZeroXInstanceKeys(options, cha, contextInterpreter, ZeroXInstanceKeys.ALLOCATIONS
        | ZeroXInstanceKeys.SMUSH_MANY | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS | ZeroXInstanceKeys.SMUSH_STRINGS
        | ZeroXInstanceKeys.SMUSH_THROWABLES);
    return zik;
  }

  /**
   * @return an object which creates contexts for call graph nodes based on the
   *         container disambiguation policy
   */
  protected ContextSelector makeContainerContextSelector(IClassHierarchy cha, ZeroXInstanceKeys keys) {
    return new ContainerContextSelector(cha, keys);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#getDefaultDispatchBoundHeuristic()
   */
  @Override
  protected byte getDefaultDispatchBoundHeuristic() {
    return AnalysisOptions.CHA_DISPATCH_BOUND;
  }
}
