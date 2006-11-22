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

import java.util.Collection;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * Basic interface for a call graph, which is a graph of CGNode
 * 
 *
 * @author Stephen Fink
 */
public interface CallGraph extends NumberedGraph<CGNode> {

  /**
   * Return the (fake) interprocedural {@link CGNode root node}
   * of the call graph.
   *
   * @return the "fake" root node the call graph
   */
  public CGNode getFakeRootNode();

  /**
   * @return an Iterator of the nodes designated as "root nodes"
   */
  public Collection<CGNode> getEntrypointNodes();

  /**
   * If you want to get <em> all </em> the nodes corresponding to 
   * a particular method, regardless of context, then use
   * {@link CGNode getNodes}
   * 
   * @return the node corresponding a method in a context
   */
  public CGNode getNode(IMethod method, Context C);

  /**
   * @param m a method reference
   * @return the set of all nodes in the call graph that represent
   * this method.
   */
  public Set<CGNode> getNodes(MethodReference m);
  
  /**
   * 
   * @param node
   * @return an object that provides an interpretation of the node.
   * This is the minimum interpreter functionality that all call graphs
   * must provide.
   */
  public SSAContextInterpreter getInterpreter(CGNode node);

  /**
   *  Dump the callgraph to the specified file in dotty(1) format.
   */
  public void dump(String filename);

  /**
   * @return the governing class hierarchy for this call graph
   */
  public ClassHierarchy getClassHierarchy();

}
