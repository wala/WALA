/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.ReflectionSpecification;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * @author sfink
 * 
 * 0-1-CFA Call graph builder, optimized to not disambiguate instances of
 * "uninteresting" types
 */
public class JSZeroXCFABuilder extends JSCFABuilder {

  public JSZeroXCFABuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache, ContextSelector appContextSelector,
      SSAContextInterpreter appContextInterpreter, ReflectionSpecification reflect, int instancePolicy) {
    super(cha, options, cache);

    SSAContextInterpreter contextInterpreter = makeDefaultContextInterpreters(appContextInterpreter, options, cha, reflect);
    setContextInterpreter(contextInterpreter);

    options.setSelector(new JavaScriptConstructTargetSelector(cha, options.getMethodTargetSelector()));

    ContextSelector def = new DefaultContextSelector(cha, options.getMethodTargetSelector());
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);

    setContextSelector(contextSelector);

    setInstanceKeys(new JavaScriptScopeMappingInstanceKeys(cha, this, new ZeroXInstanceKeys(options, cha, contextInterpreter,
        instancePolicy)));
  }

  /**
   * @param options
   *            options that govern call graph construction
   * @param cha
   *            governing class hierarchy
   * @param cl
   *            classloader that can find DOMO resources
   * @param scope
   *            representation of the analysis scope
   * @param xmlFiles
   *            set of Strings that are names of XML files holding bypass logic
   *            specifications.
   * @param dmd
   *            deployment descriptor abstraction
   * @return a 0-1-Opt-CFA Call Graph Builder.
   */
  public static JSCFABuilder make(AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha, ClassLoader cl, AnalysisScope scope,
      String[] xmlFiles, byte instancePolicy) {

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
    for (int i = 0; i < xmlFiles.length; i++) {
      com.ibm.wala.ipa.callgraph.impl.Util.addBypassLogic(options, scope, cl, xmlFiles[i], cha);
    }

    return new JSZeroXCFABuilder(cha, options, cache, null, null, options.getReflectionSpec(), instancePolicy);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.domo.ipa.callgraph.propagation.PropagationCallGraphBuilder#getDefaultDispatchBoundHeuristic()
   */
  protected byte getDefaultDispatchBoundHeuristic() {
    return AnalysisOptions.NO_DISPATCH_BOUND;
  }
}
