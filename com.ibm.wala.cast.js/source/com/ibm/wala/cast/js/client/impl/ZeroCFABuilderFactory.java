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
package com.ibm.wala.cast.js.client.impl;

import com.ibm.wala.cast.ipa.callgraph.StandardFunctionTargetSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSZeroOrOneXCFABuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * @author Julian Dolby (dolby@us.ibm.com)
 * 
 * A factory to create call graph builders using 0-CFA
 */
public class ZeroCFABuilderFactory  {

  public CallGraphBuilder make(JSAnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
    options.setSelector(new StandardFunctionTargetSelector(cha, options.getMethodTargetSelector()));

    return new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, ZeroXInstanceKeys.NONE, false);
  }
}
