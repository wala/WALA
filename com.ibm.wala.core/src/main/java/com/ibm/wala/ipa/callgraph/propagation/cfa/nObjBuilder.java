/*
 * Copyright (c) 2002 - 2020 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/** call graph builder based on object sensitivity */
public class nObjBuilder extends ZeroXCFABuilder {

  public nObjBuilder(
      int n,
      IClassHierarchy cha,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      ContextSelector appContextSelector,
      SSAContextInterpreter appContextInterpreter,
      int instancePolicy) {

    super(
        Language.JAVA,
        cha,
        options,
        cache,
        appContextSelector,
        appContextInterpreter,
        instancePolicy);

    ContextSelector def = new DefaultContextSelector(options, cha);

    ContextSelector contextSelector =
        appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);

    ContextSelector nObjContextSelector = new nObjContextSelector(n, contextSelector);

    setContextSelector(nObjContextSelector);
  }
}
