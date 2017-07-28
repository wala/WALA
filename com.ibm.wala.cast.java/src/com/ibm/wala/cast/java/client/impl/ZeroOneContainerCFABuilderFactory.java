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
package com.ibm.wala.cast.java.client.impl;

import com.ibm.wala.cast.java.ipa.callgraph.AstJavaZeroOneContainerCFABuilder;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * @author Julian Dolby (dolby@us.ibm.com)
 * 
 * A factory to create call graph builders using 0-CFA
 */
public class ZeroOneContainerCFABuilderFactory  {

  public CallGraphBuilder make(AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha, AnalysisScope scope) {
    Util.addDefaultSelectors(options, cha);
    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
    return new AstJavaZeroOneContainerCFABuilder(cha, options, cache, null, null);
  }
}
