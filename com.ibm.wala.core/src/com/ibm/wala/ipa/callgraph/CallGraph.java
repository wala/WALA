/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.graph.NumberedGraph;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/** Basic interface for a call graph, which is a graph of {@link CGNode} */
public interface CallGraph extends NumberedGraph<CGNode> {

  /**
   * Return the (fake) interprocedural {@link CGNode root node} of the call graph.
   *
   * @return the "fake" root node the call graph
   */
  public CGNode getFakeRootNode();

  CGNode getFakeWorldClinitNode();

  /** @return an Iterator of the nodes designated as "root nodes" */
  public Collection<CGNode> getEntrypointNodes();

  /**
   * If you want to get <em> all </em> the nodes corresponding to a particular method, regardless of
   * context, then use {@link CGNode getNodes}
   *
   * @return the node corresponding a method in a context
   */
  public CGNode getNode(IMethod method, Context C);

  /**
   * @param m a method reference
   * @return the set of all nodes in the call graph that represent this method.
   */
  public Set<CGNode> getNodes(MethodReference m);

  /** @return the governing class hierarchy for this call graph */
  public IClassHierarchy getClassHierarchy();

  /**
   * Return the set of CGNodes that represent possible targets of a particular call site from a
   * particular node
   */
  public Set<CGNode> getPossibleTargets(CGNode node, CallSiteReference site);

  /** @return the number of nodes that the call site may dispatch to */
  public int getNumberOfTargets(CGNode node, CallSiteReference site);

  /**
   * @return iterator of CallSiteReference, the call sites in a node that might dispatch to the
   *     target node.
   */
  Iterator<CallSiteReference> getPossibleSites(CGNode src, CGNode target);
}
