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
package com.ibm.wala.client.impl;

import com.ibm.wala.client.CallGraphBuilderFactory;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * A factory to create call graph builders using 0-C-CFA
 * 
 * @author sfink
 */
public class ZeroContainerCFABuilderFactory implements CallGraphBuilderFactory {

  public CallGraphBuilder make(AnalysisOptions options, IClassHierarchy cha, AnalysisScope scope, WarningSet warnings,
      boolean keepPointsTo) {
    return Util.makeZeroContainerCFABuilder(options, cha, scope, warnings);
  }
}
