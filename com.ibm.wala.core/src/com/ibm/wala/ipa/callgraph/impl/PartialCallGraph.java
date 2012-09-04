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
package com.ibm.wala.ipa.callgraph.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.impl.DelegatingGraph;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * a view of a portion of a call graph.
 */
public class PartialCallGraph extends DelegatingGraph<CGNode> implements CallGraph {

  protected final CallGraph cg;

  protected final Collection<CGNode> partialRoots;

  protected PartialCallGraph(CallGraph cg, Collection<CGNode> partialRoots, Graph<CGNode> partialGraph) {
    super(partialGraph);
    this.cg = cg;
    this.partialRoots = partialRoots;
  }

  /**
   * @param cg the original call graph
   * @param partialRoots roots of the new, partial graph
   * @param nodes set of nodes that will be included in the new, partial call graph
   */
  public static PartialCallGraph make(final CallGraph cg, final Collection<CGNode> partialRoots, final Collection<CGNode> nodes) {
    Graph<CGNode> partialGraph = GraphSlicer.prune(cg, new Filter<CGNode>() {
      public boolean accepts(CGNode o) {
        return nodes.contains(o);
      }
    });

    return new PartialCallGraph(cg, partialRoots, partialGraph);
  }

  /**
   * @param cg the original call graph
   * @param partialRoots roots of the new, partial graph
   * the result contains only nodes reachable from the partialRoots in the original call graph.
   */
  public static PartialCallGraph make(CallGraph cg, Collection<CGNode> partialRoots) {
    final Set<CGNode> nodes = DFS.getReachableNodes(cg, partialRoots);
    Graph<CGNode> partialGraph = GraphSlicer.prune(cg, new Filter<CGNode>() {
      public boolean accepts(CGNode o) {
        return nodes.contains(o);
      }
    });

    return new PartialCallGraph(cg, partialRoots, partialGraph);
  }

  public CGNode getFakeRootNode() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public Collection<CGNode> getEntrypointNodes() {
    return partialRoots;
  }

  public CGNode getNode(IMethod method, Context C) {
    CGNode x = cg.getNode(method, C);
    if (x == null) {
      return null;
    }
    return (containsNode(x) ? x : null);
  }

  public Set<CGNode> getNodes(MethodReference m) {
    Set<CGNode> result = HashSetFactory.make();
    for (Iterator xs = cg.getNodes(m).iterator(); xs.hasNext();) {
      CGNode x = (CGNode) xs.next();
      if (containsNode(x)) {
        result.add(x);
      }
    }

    return result;
  }

  public IClassHierarchy getClassHierarchy() {
    return cg.getClassHierarchy();
  }

  public Iterator<CGNode> iterateNodes(IntSet nodes) {
    return new FilterIterator<CGNode>(cg.iterateNodes(nodes), new Filter() {
      public boolean accepts(Object o) {
        return containsNode((CGNode) o);
      }
    });
  }

  public int getMaxNumber() {
    return cg.getMaxNumber();
  }

  public CGNode getNode(int index) {
    CGNode n = cg.getNode(index);
    return (containsNode(n) ? n : null);
  }

  public int getNumber(CGNode n) {
    return (containsNode(n) ? cg.getNumber(n) : -1);
  }

  public IntSet getSuccNodeNumbers(CGNode node) {
    assert containsNode(node);
    MutableIntSet x = IntSetUtil.make();
    for (Iterator ns = getSuccNodes(node); ns.hasNext();) {
      CGNode succ = (CGNode) ns.next();
      if (containsNode(succ)) {
        x.add(getNumber(succ));
      }
    }

    return x;
  }

  public IntSet getPredNodeNumbers(CGNode node) {
    assert containsNode(node);
    MutableIntSet x = IntSetUtil.make();
    for (Iterator ns = getPredNodes(node); ns.hasNext();) {
      CGNode pred = (CGNode) ns.next();
      if (containsNode(pred)) {
        x.add(getNumber(pred));
      }
    }

    return x;
  }

  public int getNumberOfTargets(CGNode node, CallSiteReference site) {
    return (containsNode(node) ? getPossibleTargets(node, site).size() : -1);
  }

  public Iterator<CallSiteReference> getPossibleSites(CGNode src, CGNode target) {
    return ((containsNode(src) && containsNode(target)) ? cg.getPossibleSites(src, target) : null);
  }

  public Set<CGNode> getPossibleTargets(CGNode node, CallSiteReference site) {
    if (!containsNode(node)) {
      return null;
    }
    Set<CGNode> result = HashSetFactory.make();
    for (Iterator ns = cg.getPossibleTargets(node, site).iterator(); ns.hasNext();) {
      CGNode target = (CGNode) ns.next();
      if (containsNode(target)) {
        result.add(target);
      }
    }

    return result;
  }
}
