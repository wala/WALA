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
package com.ibm.wala.ipa.callgraph;


import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

/**
 * Basic interface for an object that can build a call graph.
 */
public interface CallGraphBuilder<I extends InstanceKey> {
  /**
   * Build a call graph.
   * 
   * @param options an object representing controlling options that the call graph building algorithm needs to know.
   * @return the built call graph
   * @throws  
   */
  public CallGraph makeCallGraph(AnalysisOptions options, IProgressMonitor monitor) throws IllegalArgumentException,
      CallGraphBuilderCancelException;

  /**
   * @return the Pointer Analysis information computed as a side-effect of call graph construction.
   */
  public PointerAnalysis<I> getPointerAnalysis();

  /**
   * @return A cache of various analysis artifacts used during call graph construction.
   */
  public AnalysisCache getAnalysisCache();

  public IClassHierarchy getClassHierarchy();
  
}
