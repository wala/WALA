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

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.ReflectionSpecification;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * 0-CFA Call graph builder which analyzes calls to "container methods" in a
 * context which is defined by the receiver instance.
 * 
 * @author sfink
 */
public class ZeroContainerCFABuilder extends CFABuilder {

  /**
   * @param cha
   *          governing class hierarhcy
   * @param warnings
   *          object to track analysis warnings
   * @param options
   *          call graph construction options
   * @param appContextSelector
   *          application-specific logic to choose contexts
   * @param appContextInterpreter
   *          application-specific logic to interpret a method in context
   * @param reflect
   *          reflection specification
   * @throws IllegalArgumentException
   *           if options is null
   */
  public ZeroContainerCFABuilder(ClassHierarchy cha, WarningSet warnings, AnalysisOptions options,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, ReflectionSpecification reflect) {

    super(cha, warnings, options);
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }

    setInstanceKeys(new ZeroXInstanceKeys(options, cha, null, warnings, ZeroXInstanceKeys.NONE));

    ContextSelector def = new DefaultContextSelector(cha, options.getMethodTargetSelector());
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);

    ContainerContextSelector CCS = new ContainerContextSelector(cha, (ZeroXInstanceKeys) getInstanceKeys());
    DelegatingContextSelector DCS = new DelegatingContextSelector(CCS, contextSelector);
    setContextSelector(DCS);

    setContextInterpreter(makeDefaultContextInterpreters(appContextInterpreter, options, cha, reflect, warnings));

  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#getDefaultDispatchBoundHeuristic()
   */
  @Override
  protected byte getDefaultDispatchBoundHeuristic() {
    return AnalysisOptions.CHA_DISPATCH_BOUND;
  }
}
