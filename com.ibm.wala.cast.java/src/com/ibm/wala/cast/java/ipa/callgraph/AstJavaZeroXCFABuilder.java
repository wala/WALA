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
package com.ibm.wala.cast.java.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 0-1-CFA Call graph builder, optimized to not disambiguate instances of "uninteresting" types.
 */
public class AstJavaZeroXCFABuilder extends AstJavaCFABuilder {

  public AstJavaZeroXCFABuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, int instancePolicy) {
    super(cha, options, cache);

    SSAContextInterpreter contextInterpreter = makeDefaultContextInterpreters(appContextInterpreter, options, cha);
    setContextInterpreter(contextInterpreter);

    ContextSelector def = new DefaultContextSelector(options, cha);
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);

    setContextSelector(contextSelector);

    setInstanceKeys(new JavaScopeMappingInstanceKeys(this, new ZeroXInstanceKeys(options, cha, contextInterpreter,
        instancePolicy)));
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param cl classloader that can find DOMO resources
   * @param scope representation of the analysis scope
   * @param xmlFiles set of Strings that are names of XML files holding bypass logic specifications.
   * @return a 0-1-Opt-CFA Call Graph Builder.
   */
  public static AstJavaCFABuilder make(AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha, ClassLoader cl,
      AnalysisScope scope, String[] xmlFiles, byte instancePolicy) {

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
    for (String xmlFile : xmlFiles) {
      com.ibm.wala.ipa.callgraph.impl.Util.addBypassLogic(options, scope, cl, xmlFile, cha);
    }

    return new AstJavaZeroXCFABuilder(cha, options, cache, null, null, instancePolicy);
  }

}
