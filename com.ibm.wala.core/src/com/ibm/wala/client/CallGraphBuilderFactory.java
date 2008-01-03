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
package com.ibm.wala.client;

import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * A factory for call graph builders
 * 
 * @author sfink
 */
public interface CallGraphBuilderFactory {

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param scope
   *          representation of the analysis scope
   * @param keepPointsTo
   *          preserve PointsTo graph for posterity?
   *  
   */
  CallGraphBuilder make(AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha, AnalysisScope scope, boolean keepPointsTo);
}
