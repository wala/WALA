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
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector;
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

  private static final boolean USE_OBJECT_SENSITIVITY = false;
  

  public JSZeroOrOneXCFABuilder(IClassHierarchy cha, JSAnalysisOptions options, AnalysisCache cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, int instancePolicy, boolean doOneCFA) {
    super(cha, options, cache);

    SSAContextInterpreter contextInterpreter = makeDefaultContextInterpreters(appContextInterpreter, options, cha);
    if (options.handleCallApply()) {
      contextInterpreter = new DelegatingSSAContextInterpreter(new JavaScriptFunctionApplyContextInterpreter(options, cache),
          contextInterpreter);
    }
    setContextInterpreter(contextInterpreter);

    MethodTargetSelector targetSelector = new JavaScriptConstructTargetSelector(cha, options
        .getMethodTargetSelector());
    if (options.handleCallApply()) {
      targetSelector = new JavaScriptFunctionApplyTargetSelector(new JavaScriptFunctionDotCallTargetSelector(targetSelector));
    }
    if (options.useLoadFileTargetSelector()) {
      targetSelector = new LoadFileTargetSelector(targetSelector, this);
    }
    options.setSelector(targetSelector);

    ContextSelector def = new ContextInsensitiveSelector();
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);
//    if (!AstTranslator.NEW_LEXICAL) {
      contextSelector = new ScopeMappingKeysContextSelector(contextSelector);
//    }
    contextSelector = new JavaScriptConstructorContextSelector(contextSelector);
    if (USE_OBJECT_SENSITIVITY) {
      contextSelector = new ObjectSensitivityContextSelector(contextSelector);
    }
    if (options.handleCallApply()) {
      contextSelector = new JavaScriptFunctionApplyContextSelector(contextSelector);
    }
    if (!AstTranslator.NEW_LEXICAL) {
      contextSelector = new LexicalScopingResolverContexts(this, contextSelector);
    }
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
  public static JSCFABuilder make(JSAnalysisOptions options, AnalysisCache cache, IClassHierarchy cha, ClassLoader cl,
      AnalysisScope scope, String[] xmlFiles, byte instancePolicy, boolean doOneCFA) {
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
    for (int i = 0; i < xmlFiles.length; i++) {
      com.ibm.wala.ipa.callgraph.impl.Util.addBypassLogic(options, scope, cl, xmlFiles[i], cha);
    }

    return new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, instancePolicy, doOneCFA);
  }

}
