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

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
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
  

  public JSZeroOrOneXCFABuilder(IClassHierarchy cha, JSAnalysisOptions options, IAnalysisCacheView cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, int instancePolicy, boolean doOneCFA) {
    super(cha, options, cache);

    SSAContextInterpreter contextInterpreter = setupSSAContextInterpreter(cha, options, cache, appContextInterpreter);

    setupMethodTargetSelector(cha, options);

    setupContextSelector(options, appContextSelector, doOneCFA);

    setInstanceKeys(new JavaScriptScopeMappingInstanceKeys(cha, this, new JavaScriptConstructorInstanceKeys(new ZeroXInstanceKeys(
        options, cha, contextInterpreter, instancePolicy))));
  }

  private void setupContextSelector(JSAnalysisOptions options, ContextSelector appContextSelector, boolean doOneCFA) {
    // baseline selector
    ContextSelector def = new ContextInsensitiveSelector();
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);
    
    // JavaScriptConstructorContextSelector ensures at least a 0-1-CFA (i.e.,
    // Andersen's-style) heap abstraction. This level of heap abstraction is
    // _necessary_ for correctness (we rely on it when handling lexical scoping)
    contextSelector = new JavaScriptConstructorContextSelector(contextSelector);
    
    //contextSelector = new OneLevelForLexicalAccessFunctions(contextSelector);
    
    if (USE_OBJECT_SENSITIVITY) {
      contextSelector = new ObjectSensitivityContextSelector(contextSelector);
    }
    if (options.handleCallApply()) {
      contextSelector = new JavaScriptFunctionApplyContextSelector(contextSelector);
    }
    if (doOneCFA) {
      contextSelector = new nCFAContextSelector(1, contextSelector);
    }
    setContextSelector(contextSelector);
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

  private SSAContextInterpreter setupSSAContextInterpreter(IClassHierarchy cha, JSAnalysisOptions options, IAnalysisCacheView cache,
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
   * @return a 0-1-Opt-CFA Call Graph Builder.
   */
  public static JSCFABuilder make(JSAnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha, ClassLoader cl,
      AnalysisScope scope, String[] xmlFiles, byte instancePolicy, boolean doOneCFA) {
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
    for (String xmlFile : xmlFiles) {
      com.ibm.wala.ipa.callgraph.impl.Util.addBypassLogic(options, scope, cl, xmlFile, cha);
    }

    return new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, instancePolicy, doOneCFA);
  }

}
