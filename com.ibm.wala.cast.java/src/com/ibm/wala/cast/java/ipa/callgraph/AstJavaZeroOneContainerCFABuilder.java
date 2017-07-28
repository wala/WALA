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
package com.ibm.wala.cast.java.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ContainerContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 
 * 0-1-CFA Call graph builder which analyzes calls to "container methods" in a context which is defined by the receiver
 * instance.
 * 
 * @author sfink
 */
public class AstJavaZeroOneContainerCFABuilder extends AstJavaCFABuilder {

  /**
   * @param cha governing class hierarchy
   * @param options call graph construction options
   * @param appContextSelector application-specific logic to choose contexts
   * @param appContextInterpreter application-specific logic to interpret a method in context
   */
  public AstJavaZeroOneContainerCFABuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter) {
    super(cha, options, cache);

    ContextSelector def = new DefaultContextSelector(options, cha);
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);

    SSAContextInterpreter contextInterpreter = makeDefaultContextInterpreters(appContextInterpreter, options, cha);
    setContextInterpreter(contextInterpreter);

    ZeroXInstanceKeys zik = makeInstanceKeys(cha, options, contextInterpreter);
    setInstanceKeys(new JavaScopeMappingInstanceKeys(this, zik));

    ContextSelector CCS = makeContainerContextSelector(cha, zik);
    DelegatingContextSelector DCS = new DelegatingContextSelector(CCS, contextSelector);
    setContextSelector(DCS);
  }

  protected ZeroXInstanceKeys makeInstanceKeys(IClassHierarchy cha, AnalysisOptions options,
      SSAContextInterpreter contextInterpreter) {
    ZeroXInstanceKeys zik = new ZeroXInstanceKeys(options, cha, contextInterpreter, ZeroXInstanceKeys.ALLOCATIONS
        | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS | ZeroXInstanceKeys.SMUSH_STRINGS | ZeroXInstanceKeys.SMUSH_MANY
        | ZeroXInstanceKeys.SMUSH_THROWABLES);
    return zik;
  }

  /**
   * @param cha
   * @param keys
   * @return an object which creates contexts for call graph nodes based on the container disambiguation policy
   */
  protected ContextSelector makeContainerContextSelector(IClassHierarchy cha, ZeroXInstanceKeys keys) {
    return new ContainerContextSelector(cha, keys);
  }

}
