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

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 0-X-CFA Call graph builder which analyzes calls to "container methods" in a context which is defined by the receiver instance.
 */
public class ZeroXContainerCFABuilder extends ZeroXCFABuilder {

  /**
   * @param cha governing class hierarchy
   * @param options call graph construction options
   * @param appContextSelector application-specific logic to choose contexts
   * @param appContextInterpreter application-specific logic to interpret a method in context
   * @throws IllegalArgumentException if options is null
   */
  public ZeroXContainerCFABuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, int instancePolicy) {

    super(cha, options, cache, appContextSelector, appContextInterpreter, instancePolicy);

    ContextSelector CCS = makeContainerContextSelector(cha, (ZeroXInstanceKeys) getInstanceKeys());
    DelegatingContextSelector DCS = new DelegatingContextSelector(CCS, contextSelector);
    setContextSelector(DCS);
  }

  /**
   * @return an object which creates contexts for call graph nodes based on the container disambiguation policy
   */
  protected ContextSelector makeContainerContextSelector(IClassHierarchy cha, ZeroXInstanceKeys keys) {
    return new ContainerContextSelector(cha, keys);
  }

}
