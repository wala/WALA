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

import java.util.ArrayList;
import java.util.Collection;

import com.ibm.wala.cast.ipa.callgraph.LexicalScopingResolverContexts;
import com.ibm.wala.cast.ipa.callgraph.ScopeMappingKeysContextSelector;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ComposedContextSelector;
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

    SSAContextInterpreter contextInterpreter = setupSSAContextInterpreter(cha, options, cache, appContextInterpreter);

    setupMethodTargetSelector(cha, options);

    setupContextSelector(options, appContextSelector, doOneCFA);

    setInstanceKeys(new JavaScriptScopeMappingInstanceKeys(cha, this, new JavaScriptConstructorInstanceKeys(new ZeroXInstanceKeys(
        options, cha, contextInterpreter, instancePolicy))));
  }

  private void setupContextSelector(JSAnalysisOptions options, ContextSelector appContextSelector, boolean doOneCFA) {
    Collection<ContextSelector> selectors = new ArrayList<ContextSelector>();
    ContextSelector def = new ContextInsensitiveSelector();
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);
    selectors.add(contextSelector);
//    if (!AstTranslator.NEW_LEXICAL) {
      selectors.add(new ScopeMappingKeysContextSelector());
//    }
    selectors.add(new JavaScriptConstructorContextSelector());
    if (USE_OBJECT_SENSITIVITY) {
      selectors.add(new ObjectSensitivityContextSelector());
    }
    if (options.handleCallApply()) {
      selectors.add(new JavaScriptFunctionApplyContextSelector());
    }
    if (!AstTranslator.NEW_LEXICAL) {
      selectors.add(new LexicalScopingResolverContexts(this));
    }
    if (doOneCFA) {
      selectors.add(new nCFAContextSelector(1, new ContextInsensitiveSelector()));
    }
    setContextSelector(new ComposedContextSelector(selectors));
  }

  private void setupMethodTargetSelector(IClassHierarchy cha, JSAnalysisOptions options) {
    MethodTargetSelector targetSelector = new JavaScriptConstructTargetSelector(cha, options
        .getMethodTargetSelector());
    if (options.handleCallApply()) {
      targetSelector = new JavaScriptFunctionApplyTargetSelector(new JavaScriptFunctionDotCallTargetSelector(targetSelector));
    }
    if (options.useLoadFileTargetSelector()) {
      targetSelector = new LoadFileTargetSelector(targetSelector, this);
    }
    options.setSelector(targetSelector);
  }

  private SSAContextInterpreter setupSSAContextInterpreter(IClassHierarchy cha, JSAnalysisOptions options, AnalysisCache cache,
      SSAContextInterpreter appContextInterpreter) {
    SSAContextInterpreter contextInterpreter = makeDefaultContextInterpreters(appContextInterpreter, options, cha);
    if (options.handleCallApply()) {
      contextInterpreter = new DelegatingSSAContextInterpreter(new JavaScriptFunctionApplyContextInterpreter(options, cache),
          contextInterpreter);
    }
    setContextInterpreter(contextInterpreter);
    return contextInterpreter;
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
