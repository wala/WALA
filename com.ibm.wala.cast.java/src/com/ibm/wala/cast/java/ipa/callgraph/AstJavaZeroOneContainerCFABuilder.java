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
package com.ibm.wala.cast.java.ipa.callgraph;

import com.ibm.wala.analysis.reflection.FactoryBypassInterpreter;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.ReflectionSpecification;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * 0-1-CFA Call graph builder which analyzes calls to "container methods" in a
 * context which is defined by the receiver instance.
 * 
 * @author sfink
 */
public class AstJavaZeroOneContainerCFABuilder extends AstJavaCFABuilder {

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
   */
  public AstJavaZeroOneContainerCFABuilder(
		  ClassHierarchy cha, 
		  WarningSet warnings, 
		  AnalysisOptions options,
		  ContextSelector appContextSelector,
		  SSAContextInterpreter appContextInterpreter, 
		  ReflectionSpecification reflect) 
  {
    super(cha, warnings, options);

    ContextSelector def = new DefaultContextSelector(cha, options.getMethodTargetSelector());
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);

    SSAContextInterpreter contextInterpreter = 
      makeDefaultContextInterpreters(appContextInterpreter, options, cha, reflect, warnings);
    setContextInterpreter(contextInterpreter);

    ZeroXInstanceKeys zik = makeInstanceKeys(cha, warnings, options, contextInterpreter);
    setInstanceKeys(zik);

    ContextSelector CCS = makeContainerContextSelector(cha,(ZeroXInstanceKeys) getInstanceKeys());
    DelegatingContextSelector DCS = new DelegatingContextSelector(CCS, contextSelector);
    setContextSelector(DCS);
  }

  protected ZeroXInstanceKeys makeInstanceKeys(ClassHierarchy cha, WarningSet warnings, AnalysisOptions options, SSAContextInterpreter contextInterpreter) {
    ZeroXInstanceKeys zik = new ZeroXInstanceKeys(options, cha, contextInterpreter, warnings, ZeroXInstanceKeys.ALLOCATIONS
        | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS 
	| ZeroXInstanceKeys.SMUSH_STRINGS
	| ZeroXInstanceKeys.SMUSH_MANY
        | ZeroXInstanceKeys.SMUSH_THROWABLES);
    return zik;
  }
  
  /**
   * @param cha
   * @param keys
   * @return an object which creates contexts for call graph nodes based on the container disambiguation policy
   */
  protected ContextSelector makeContainerContextSelector(ClassHierarchy cha, ZeroXInstanceKeys keys) {
    return new ContainerContextSelector(cha,keys );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#getDefaultDispatchBoundHeuristic()
   */
  protected byte getDefaultDispatchBoundHeuristic() {
    return AnalysisOptions.CHA_DISPATCH_BOUND;
  }
}
