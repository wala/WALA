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

import java.util.Iterator;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.graph.INodeWithNumber;

/**
 * Basic interface for a node in a call graph.
 *
 * @author Stephen Fink
 */

public interface CGNode extends INodeWithNumber, ContextItem {
  /**
   * Return the {@link IMethod method} this CGNode represents.
   * This value will never be <code>null</code>.
   *
   * @return the target IMethod for this CGNode.
   */
  public IMethod getMethod();

  /**
   * Return the {@link Context context} this CGNode represents.
   * This value will never be <code>null</code>.
   *
   * @return the Context for this CGNode.
   */
  public Context getContext();

  /**
   * @return Iterator of CallSiteReference
   */
  Iterator<CallSiteReference> iterateSites();

  /**
   * This is for use only by call graph builders ... not by the general
   * public.  Clients should not use this.
   * 
   * Record that a particular call site might resolve to a call to a 
   * particular target node.  Returns true if this is a new target
   */
  @Deprecated
  public boolean addTarget(CallSiteReference site, CGNode target);

//  /**
//   * @return the number of nodes that the call site current may resolve
//   * to
//   */
//  public int getNumberOfTargets(CallSiteReference site);
  
//  /**
//   * @return the call graph in which this node dwells
//   */
//  public CallGraph getCallGraph();
  
  /**
   * @return the "default" IR for this node used by the governing call graph
   */
  public IR getIR();
  
  /**
   * @return DefUse for the "default" IR for this node used by the governing call graph
   */
  public DefUse getDU();
  
  /**
   * @return a CFG that represents the node, or null if it's an unmodelled native method
   */
  public ControlFlowGraph<ISSABasicBlock> getCFG();
  
  /**
   * @return an Iterator of the types that may be allocated by a given
   * method in a given context.
   */
  public Iterator<NewSiteReference> iterateNewSites();
  /**
   * @return an Iterator of the call statements that may execute
   * in a given method for a given context
   */
  public Iterator<CallSiteReference> iterateCallSites();
}
