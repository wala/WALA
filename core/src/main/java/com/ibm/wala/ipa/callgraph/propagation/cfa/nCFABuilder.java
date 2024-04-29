/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.analysis.reflection.ReflectionContextInterpreter;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ClassBasedInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;

/**
 * nCFA Call graph builder. Note that by default, this builder uses a {@link ClassBasedInstanceKeys}
 * heap model.
 */
public class nCFABuilder extends SSAPropagationCallGraphBuilder {

  public nCFABuilder(
      int n,
      IMethod abstractRootMethod,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      ContextSelector appContextSelector,
      SSAContextInterpreter appContextInterpreter) {

    super(abstractRootMethod, options, cache, new DefaultPointerKeyFactory());
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }

    setInstanceKeys(new ClassBasedInstanceKeys(options, cha));

    ContextSelector def = new DefaultContextSelector(options, cha);
    ContextSelector contextSelector =
        appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);
    contextSelector = new nCFAContextSelector(n, contextSelector);
    setContextSelector(contextSelector);

    SSAContextInterpreter defI = new DefaultSSAInterpreter(options, cache);
    defI =
        new DelegatingSSAContextInterpreter(
            ReflectionContextInterpreter.createReflectionContextInterpreter(
                cha, options, getAnalysisCache()),
            defI);
    SSAContextInterpreter contextInterpreter =
        appContextInterpreter == null
            ? defI
            : new DelegatingSSAContextInterpreter(appContextInterpreter, defI);
    setContextInterpreter(contextInterpreter);
  }
}
