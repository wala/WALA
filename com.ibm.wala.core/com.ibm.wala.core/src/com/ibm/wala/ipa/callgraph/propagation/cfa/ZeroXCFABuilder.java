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
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.ReflectionSpecification;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * 0-1-CFA Call graph builder, optimized to not disambiguate instances of
 * "uninteresting" types
 * 
 * @author sfink
 */
public class ZeroXCFABuilder extends CFABuilder {

  private final int instancePolicy;

  public ZeroXCFABuilder(ClassHierarchy cha, WarningSet warnings, AnalysisOptions options, ContextSelector appContextSelector,
      SSAContextInterpreter appContextInterpreter, ReflectionSpecification reflect, int instancePolicy) {
    super(cha, warnings, options);

    this.instancePolicy = instancePolicy;

    ContextSelector def = new DefaultContextSelector(cha, options.getMethodTargetSelector());
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);
    setContextSelector(contextSelector);

    SSAContextInterpreter c = new DefaultSSAInterpreter(options, cha, warnings);
    c = new DelegatingSSAContextInterpreter(new FactoryBypassInterpreter(options, cha, reflect, warnings), c);
    SSAContextInterpreter contextInterpreter = new DelegatingSSAContextInterpreter(appContextInterpreter, c);
    setContextInterpreter(contextInterpreter);

    ZeroXInstanceKeys zik = makeInstanceKeys(cha,warnings,options,contextInterpreter,instancePolicy);
    setInstanceKeys(zik);
  }
  
  /**
   * subclasses can override as desired
   */
  protected ZeroXInstanceKeys makeInstanceKeys(ClassHierarchy cha, WarningSet warnings, AnalysisOptions options, SSAContextInterpreter contextInterpreter, int instancePolicy) {
    ZeroXInstanceKeys zik = new ZeroXInstanceKeys(options, cha, contextInterpreter, warnings, instancePolicy);
    return zik;
  }

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param cl
   *          classloader that can find WALA resources
   * @param scope
   *          representation of the analysis scope
   * @param xmlFiles
   *          set of Strings that are names of XML files holding bypass logic
   *          specifications.
   * @return a 0-1-Opt-CFA Call Graph Builder.
   */
  public static CFABuilder make(AnalysisOptions options, ClassHierarchy cha, ClassLoader cl, AnalysisScope scope,
      String[] xmlFiles, WarningSet warnings, byte instancePolicy) {

    Util.addDefaultSelectors(options, cha, warnings);
    for (int i = 0; i < xmlFiles.length; i++) {
      Util.addBypassLogic(options, scope, cl, xmlFiles[i], cha);
    }

    return new ZeroXCFABuilder(cha, warnings, options, null, null, options.getReflectionSpec(), instancePolicy);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#getDefaultDispatchBoundHeuristic()
   */
  protected byte getDefaultDispatchBoundHeuristic() {
    switch (instancePolicy) {
    case ZeroXInstanceKeys.ALLOCATIONS:
      return AnalysisOptions.CHA_DISPATCH_BOUND;
    default:
      return AnalysisOptions.NO_DISPATCH_BOUND;
    }
  }
}
