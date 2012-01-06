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

import com.ibm.wala.cast.ipa.callgraph.LexicalScopingResolverContexts;
import com.ibm.wala.cast.ipa.callgraph.ScopeMappingKeysContextSelector;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 0-x-CFA Call graph builder, optimized to not disambiguate instances of
 * "uninteresting" types
 */
public class JSZeroOrOneXCFABuilder extends JSCFABuilder {

  private static final boolean HANDLE_FUNCTION_APPLY = true;

  public JSZeroOrOneXCFABuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, int instancePolicy, boolean doOneCFA) {
    super(cha, options, cache);

    SSAContextInterpreter contextInterpreter = makeDefaultContextInterpreters(appContextInterpreter, options, cha);
    if (HANDLE_FUNCTION_APPLY) {
      contextInterpreter = new DelegatingSSAContextInterpreter(new JavaScriptFunctionApplyContextInterpreter(options, cache),
          contextInterpreter);
    }
    setContextInterpreter(contextInterpreter);

    options.setSelector(new JavaScriptFunctionDotCallTargetSelector(cha, new JavaScriptConstructTargetSelector(cha, options
        .getMethodTargetSelector())));
    options.setSelector(new LoadFileTargetSelector(options.getMethodTargetSelector(), this));

    ContextSelector def = new DefaultContextSelector(options, cha);
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);
    contextSelector = new ScopeMappingKeysContextSelector(contextSelector);
    contextSelector = new JavaScriptConstructorContextSelector(contextSelector);
    if (HANDLE_FUNCTION_APPLY) {
      contextSelector = new JavaScriptFunctionApplyContextSelector(contextSelector);
    }
    contextSelector = new LexicalScopingResolverContexts(this, contextSelector);
    if (doOneCFA) {
      contextSelector = new nCFAContextSelector(1, contextSelector);
    }
    setContextSelector(contextSelector);

    setInstanceKeys(new JavaScriptScopeMappingInstanceKeys(cha, this, new JavaScriptConstructorInstanceKeys(new ZeroXInstanceKeys(
        options, cha, contextInterpreter, instancePolicy))));
  }

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param cl
   *          classloader that can find DOMO resources
   * @param scope
   *          representation of the analysis scope
   * @param xmlFiles
   *          set of Strings that are names of XML files holding bypass logic
   *          specifications.
   * @param dmd
   *          deployment descriptor abstraction
   * @return a 0-1-Opt-CFA Call Graph Builder.
   */
  public static JSCFABuilder make(AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha, ClassLoader cl,
      AnalysisScope scope, String[] xmlFiles, byte instancePolicy, boolean doOneCFA) {
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
    for (int i = 0; i < xmlFiles.length; i++) {
      com.ibm.wala.ipa.callgraph.impl.Util.addBypassLogic(options, scope, cl, xmlFiles[i], cha);
    }

    return new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, instancePolicy, doOneCFA);
  }

}
