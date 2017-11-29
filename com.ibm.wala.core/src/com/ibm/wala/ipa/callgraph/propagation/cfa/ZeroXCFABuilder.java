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

import com.ibm.wala.analysis.reflection.ReflectionContextInterpreter;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 0-1-CFA Call graph builder, optimized to not disambiguate instances of "uninteresting" types.
 */
public class ZeroXCFABuilder extends SSAPropagationCallGraphBuilder {

  public ZeroXCFABuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache, ContextSelector appContextSelector,
      SSAContextInterpreter appContextInterpreter, int instancePolicy) {

    super(cha, options, cache, new DefaultPointerKeyFactory());

    ContextSelector def = new DefaultContextSelector(options, cha);
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);
    setContextSelector(contextSelector);

    SSAContextInterpreter c = new DefaultSSAInterpreter(options, cache);
    c = new DelegatingSSAContextInterpreter(ReflectionContextInterpreter.createReflectionContextInterpreter(cha, options,
        getAnalysisCache()), c);
    SSAContextInterpreter contextInterpreter = appContextInterpreter == null ? c : new DelegatingSSAContextInterpreter(
        appContextInterpreter, c);
    setContextInterpreter(contextInterpreter);

    ZeroXInstanceKeys zik = makeInstanceKeys(cha, options, contextInterpreter, instancePolicy);
    setInstanceKeys(zik);
  }

  /**
   * subclasses can override as desired
   */
  protected ZeroXInstanceKeys makeInstanceKeys(IClassHierarchy cha, AnalysisOptions options,
      SSAContextInterpreter contextInterpreter, int instancePolicy) {
    ZeroXInstanceKeys zik = new ZeroXInstanceKeys(options, cha, contextInterpreter, instancePolicy);
    return zik;
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param cl classloader that can find WALA resources
   * @param scope representation of the analysis scope
   * @param xmlFiles set of Strings that are names of XML files holding bypass logic specifications.
   * @return a 0-1-Opt-CFA Call Graph Builder.
   * @throws IllegalArgumentException if options is null
   * @throws IllegalArgumentException if xmlFiles == null
   */
  public static SSAPropagationCallGraphBuilder make(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha,
      ClassLoader cl, AnalysisScope scope, String[] xmlFiles, byte instancePolicy) throws IllegalArgumentException {

    if (xmlFiles == null) {
      throw new IllegalArgumentException("xmlFiles == null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    Util.addDefaultSelectors(options, cha);
    for (String xmlFile : xmlFiles) {
      Util.addBypassLogic(options, scope, cl, xmlFile, cha);
    }

    return new ZeroXCFABuilder(cha, options, cache, null, null, instancePolicy);
  }

  public static ZeroXCFABuilder make(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, int instancePolicy) throws IllegalArgumentException {
    if (options == null) {
      throw new IllegalArgumentException("options == null");
    }
    return new ZeroXCFABuilder(cha, options, cache, appContextSelector, appContextInterpreter, instancePolicy);
  }

}
