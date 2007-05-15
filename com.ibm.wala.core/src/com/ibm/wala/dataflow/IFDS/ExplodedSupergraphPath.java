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
package com.ibm.wala.dataflow.IFDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.CollectionFilter;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.BFSPathFinder;

/**
 * 
 * A realizable path in the exploded supergraph
 * 
 * @author sfink
 */
public class ExplodedSupergraphPath<T> {

  private static final boolean DEBUG = false;

  /**
   * List<ExplodedSupergraphNode>
   */
  private final List<ExplodedSupergraphNode<T>> outermostList;

  /**
   * Map: Pair<ExplodedSupegraphNode,ExplodedSupergraphNode> -> List<ExplodedSupergraphNode>
   * for any call-to-return edge, we memoize a List which holds a SLVP by which
   * the callee gets to the return site.
   */
  private final Map<Pair, List<ExplodedSupergraphNode<T>>> edge2SLVP = HashMapFactory.make();

  /**
   * Should we skip over boring calls when iterating?
   */
  private boolean skipBoringCalls = false;

  private final ExplodedSupergraphWithSummaryEdges<T> esg;

  /**
   * @param nodeList
   */
  private ExplodedSupergraphPath(List<ExplodedSupergraphNode<T>> nodeList, ExplodedSupergraphWithSummaryEdges<T> esg) {
    this.outermostList = nodeList;
    this.esg = esg;
  }

  /**
   * @author sfink
   * 
   * warning: these paths can be exponentially long
   */
  private final class PathIterator implements Iterator {

    private Iterator it;

    public void remove() {
      Assertions.UNREACHABLE();
    }

    PathIterator() {
      // Trace.println(ExplodedSupergraphPath.this);
      // callStack holds the set of caller nodes <CGNode> currently on the call
      // stack
      Stack<CGNode> callStack = new Stack<CGNode>();
      List<ExplodedSupergraphNode<T>> L = new ArrayList<ExplodedSupergraphNode<T>>(outermostList);
      for (int i = 0; i < L.size() - 1; i++) {
        ExplodedSupergraphNode<T> src =  L.get(i);
        ExplodedSupergraphNode<T> dest =  L.get(i + 1);
        if (esg.getSupergraph().isExit(src.getSupergraphNode())) {
          if (!callStack.isEmpty()) {
            callStack.pop();
          }
        } else {
          if (skipBoringCalls && src.getFact() == dest.getFact()) {
            continue;
          } else if (esg.getSupergraph().isCall(src.getSupergraphNode())
              && esg.getSupergraph().getProcOf(src.getSupergraphNode()).equals(
                  esg.getSupergraph().getProcOf(dest.getSupergraphNode()))) {
            CGNode srcNode = (CGNode) esg.getSupergraph().getProcOf(src.getSupergraphNode());
            // avoid recursive path blowup
            if (!callStack.contains(srcNode)) {
              callStack.push(srcNode);
              // splice the sublist into the main list.
              List slvp = findOrCreateSLVP(src, dest, callStack);
              if (slvp != null) {
                L.addAll(i + 1, findOrCreateSLVP(src, dest, callStack));
              }
            }
          }
        }
      }
      it = L.iterator();
    }

    public boolean hasNext() {
      return it.hasNext();
    }

    public Object next() {
      return it.next();
    }
  }

  /**
   * @param src
   * @param dest
   * @param callStack Stack<CGNode> which should not be traversed into
   * @return null if none found
   */
  private List<ExplodedSupergraphNode<T>> findOrCreateSLVP(ExplodedSupergraphNode<T> src, ExplodedSupergraphNode<T> dest, Stack<CGNode> callStack) {
    Pair p = new Pair<ExplodedSupergraphNode,ExplodedSupergraphNode>(src, dest);
    List<ExplodedSupergraphNode<T>> l = edge2SLVP.get(p);
    // a bit of a hack ... give up on some memoization
    if (l != null && !validInCallStack(l, callStack)) {
      l = null;
    }
    if (l == null) {
      l = computeSummarySLVP(src, dest, callStack);
      if (l != null) {
        // we might fail to find a path due to limitations in
        // how we cope with reflection
        edge2SLVP.put(p, l);
      }
    }
    return l;
  }

  /**
   * a List of nodes is "valid" for a particular callStack iff none of the call
   * nodes in the list originate from a node in the call Stack
   * 
   * @param l
   *          a list of ExplodedSupergraphNode
   * @param callStack
   *          Collection<CGNode>
   */
  private boolean validInCallStack(List<ExplodedSupergraphNode<T>> l, Stack<CGNode> callStack) {
    Iterator<ExplodedSupergraphNode<T>> it = l.iterator();
    while (it.hasNext()) {
      ExplodedSupergraphNode<T> src =  it.next();
      if (callStack.contains(esg.getSupergraph().getProcOf(src.getSupergraphNode()))) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return Iterator<ExplodedSupergraphNode>
   */
  public Iterator iterator() {
    return new PathIterator();
  }

  /**
   * A filter which accepts exploded supergraph nodes with the universal (0)
   * factoid.
   */
  private final static Filter zeroFactFilter = new Filter() {
    public boolean accepts(Object o) {
      ExplodedSupergraphNode node = (ExplodedSupergraphNode) o;
      return node.getFact() == 0;
    }

  };

  /**
   * This object traverses an exploded supergraph with summary edges from a set
   * of sources, until it finds a sink.
   * 
   * During this traversal, it will <em>NOT</em> traverse interprocedural
   * edges
   * 
   * @author sfink
   * 
   */
  static class SLVPFinder<T> extends BFSPathFinder<ExplodedSupergraphNode<T>> {
    final ExplodedSupergraphWithSummaryEdges esg;

    final Collection<CGNode> exclusions;

    SLVPFinder(ExplodedSupergraphWithSummaryEdges<T> esg, Collection<ExplodedSupergraphNode<T>> sources, Collection<ExplodedSupergraphNode<T>> sinks, Collection<CGNode> exclusions) {
      super(esg, sources.iterator(), new CollectionFilter(sinks));
      this.esg = esg;
      this.exclusions = exclusions;
      // Trace.println("FINDER");
      // Trace.printCollection("sources", sources);
      // Trace.printCollection("sinks", sinks);
      // Trace.printCollection("exclusions", exclusions);
    }

    /*
     * (non-Javadoc)
     * 
     */
    protected Iterator<ExplodedSupergraphNode<T>> getConnected(final ExplodedSupergraphNode<T> n) {

      return new FilterIterator<ExplodedSupergraphNode<T>>(super.getConnected(n), new Filter() {
        @SuppressWarnings("unchecked")
        public boolean accepts(Object o) {
          ExplodedSupergraphNode<T> dest = (ExplodedSupergraphNode<T>) o;
          // if (exclusions.contains(new Pair(src, dest))) {
          // Trace.println("exclude " + src + " , " + dest);
          // }
          return sameProc(n, dest) && !exclusions.contains(esg.getSupergraph().getProcOf(n.getSupergraphNode()));
        }
      });

    }

    private final boolean sameProc(ExplodedSupergraphNode<T> a, ExplodedSupergraphNode<T> b) {
      final PartiallyCollapsedSupergraph supergraph = (PartiallyCollapsedSupergraph) esg.getSupergraph();
      return supergraph.getProcOf(a.getSupergraphNode()).equals(supergraph.getProcOf(b.getSupergraphNode()));
    }

    /*
     * (non-Javadoc)
     * 
     */
    public List<ExplodedSupergraphNode<T>> find() {
      List<ExplodedSupergraphNode<T>> L = super.find();
      if (L == null) {
        return L;
      } else {
        Collections.reverse(L);
        return L;
      }
    }
  }

  /**
   * This object traverses an exploded supergraph with summary edges
   * <em>backwards</em> from a sink, until it finds an node with the universal
   * (0) factoid.
   * 
   * During this traversal, it will <em>NOT</em> traverse return edges from a
   * caller into the callee. Thus the returned path will only go UP the call
   * stack, and never down.
   * 
   * Note that we're guaranteed to find some universal factoid since <main,0>
   * exists.
   * 
   * @author sfink
   * 
   */
  static class NoReturnBackwardsPathFinder<T> extends BFSPathFinder<ExplodedSupergraphNode<T>> {
    final ExplodedSupergraphWithSummaryEdges<T> esg;

    NoReturnBackwardsPathFinder(ExplodedSupergraphWithSummaryEdges<T> esg, ExplodedSupergraphNode<T> sink) {
      super(GraphInverter.invert(esg), Collections.singleton(sink).iterator(), zeroFactFilter);
      this.esg = esg;
    }

    /*
     * (non-Javadoc)
     * 
     */
    protected Iterator<ExplodedSupergraphNode<T>> getConnected(ExplodedSupergraphNode<T> n) {
      ExplodedSupergraphNode src = (ExplodedSupergraphNode) n;
      PartiallyCollapsedSupergraph supergraph = (PartiallyCollapsedSupergraph) esg.getSupergraph();
      HashSet<ExplodedSupergraphNode<T>> result = new HashSet<ExplodedSupergraphNode<T>>(esg.getPredNodeCount(n));
      // add facts from non-call exploded supergraph edges
      for (Iterator<? extends ExplodedSupergraphNode<T>> it = super.getConnected(n); it.hasNext();) {
        ExplodedSupergraphNode<T> dest = it.next();
        // remember that we're traversing the graph backwards!
        switch (supergraph.classifyEdge(dest.getSupergraphNode(), src.getSupergraphNode())) {
        case ISupergraph.RETURN_EDGE:
          if (DEBUG) {
            Trace.println("Exclude edge " + src + " " + dest);
          }
          // do nothing
          break;
        default:
          result.add(dest);
          break;
        }
      }
      if (DEBUG) {
        Trace.printCollection("getConnected " + n, result);
        if (result.size() == 0) {
          if (n.toString().indexOf("BB[SSA]0") > -1) {
            if (n.toString().indexOf("k.read()I") > -1) {
              Iterator x = super.getConnected(n);
              System.err.println(x);
            }
          }
        }
      }
      return result.iterator();
    }
  }

  /**
   * Find a realizable path in the exploded supergraph to a sink node, from any
   * exploded supergraph node which represents the universal (0) factoid.
   * 
   * @return an ExplodedSupergraphPath found, or null if not found.
   */
  public static <T> ExplodedSupergraphPath<T> findRealizablePath(ExplodedSupergraphWithSummaryEdges<T> esg, ExplodedSupergraphNode<T> sink) {
    BFSPathFinder<ExplodedSupergraphNode<T>> backwardsFinder = new NoReturnBackwardsPathFinder<T>(esg, sink);
    if (DEBUG) {
      Trace.println("find path to sink " + sink);
    }

    List<ExplodedSupergraphNode<T>> L = backwardsFinder.find();

    if (DEBUG) {
      Trace.println("got backwards path " + new ExplodedSupergraphPath<T>(L, esg));
    }
    if (L == null) {
      return null;
    }
    ExplodedSupergraphPath<T> result = new ExplodedSupergraphPath<T>(L, esg);
    return result;
  }

  /**
   * for a call-to-return edge <src,dest>, return a List which holds a SLVP by
   * which the callee gets to the return site.
   * 
   * Note: During the traversal, exclude any edges which are already in the call
   * stack.
   * 
   * @return null if search fails to find a path
   * 
   */
  private List<ExplodedSupergraphNode<T>> computeSummarySLVP(ExplodedSupergraphNode<T> src, ExplodedSupergraphNode<T> dest, Collection<CGNode> callStack) {

    // calledTargets := exploded nodes that are reached from the src by a
    // call.
    HashSet<ExplodedSupergraphNode<T>> calledTargets = HashSetFactory.make(3);
    for (Iterator<ExplodedSupergraphNode<T>> it = esg.getSuccNodes(src); it.hasNext();) {
      ExplodedSupergraphNode<T> target = it.next();
      if (esg.getSupergraph().classifyEdge(src.getSupergraphNode(), target.getSupergraphNode()) == ISupergraph.CALL_EDGE) {
        calledTargets.add(target);
      }
    }

    HashSet<ExplodedSupergraphNode<T>> calledExitNodes = HashSetFactory.make(3);
    for (Iterator<ExplodedSupergraphNode<T>> it = esg.getPredNodes(dest); it.hasNext();) {
      ExplodedSupergraphNode<T> target = it.next();
      if (esg.getSupergraph().classifyEdge(target.getSupergraphNode(), dest.getSupergraphNode()) == ISupergraph.RETURN_EDGE) {
        calledExitNodes.add(target);
      }
    }

    if (calledTargets.isEmpty() || calledExitNodes.isEmpty()) {
      return null;
    } else {
      if (DEBUG) {
        Trace.println("fill in summary: " + src + " -> " + dest);
        Trace.printCollection("calledTargets ", calledTargets);
      }
      // find a path from any calledTarget to the dest.
      SLVPFinder<T> innerFinder = new SLVPFinder<T>(esg, calledTargets, calledExitNodes, callStack);
      List<ExplodedSupergraphNode<T>> subList = innerFinder.find();
      // under current algorithm subList might be null due to
      // failures to handle reflection well.
      // if (Assertions.verifyAssertions) {
      // Assertions._assert(subList != null);
      // }
      if (DEBUG) {
        if (subList == null) {
          Trace.println("null sublist");
        } else {
          Trace.printCollection("subList ", subList);
        }
      }
      return subList;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {

    StringBuffer result = new StringBuffer("Outermost List: \n");
    if (outermostList == null) {
      return "null outermost list";
    }
    appendNumberedList(result, outermostList.iterator());
    for (Iterator it = edge2SLVP.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      Pair p = (Pair) e.getKey();
      result.append("SLVP for " + p + "\n");
      List l = (List) e.getValue();
      appendNumberedList(result, l.iterator());
    }
    return result.toString();
  }

  private void appendNumberedList(StringBuffer result, Iterator it) {
    int i = 0;
    while (it.hasNext()) {
      i++;
      ExplodedSupergraphNode n = (ExplodedSupergraphNode) it.next();
      result.append(i + "   " + n + "\n");
    }
  }

  /**
   * Create a brief(er) summary of a path, which includes: 1) only call and
   * return nodes, and 2) excludes call/return pairs in which no state
   * transitions occur
   */
  public static <T> ExplodedSupergraphPath<T> summarize(ISupergraph<T,?> supergraph, ExplodedSupergraphPath<T> path) {
    pruneForCallReturn(supergraph, path);
    // System.err.println("pruned path A: " + p);
    pruneBoringCalls(supergraph, path);
    return path;
  }

  /**
   * Create a brief(er) summary of a path, which excludes call/return pairs in
   * which no state transitions occur
   * @throws IllegalArgumentException  if path is null
   */
  public static <T> void pruneBoringCalls(ISupergraph supergraph, ExplodedSupergraphPath<T> path) {
    if (path == null) {
      throw new IllegalArgumentException("path is null");
    }
    path.skipBoringCalls = true;
  }

  /**
   * Create a brief(er) summary of a path, which includes only call and return
   * nodes
   */
  public static <T> void pruneForCallReturn(ISupergraph<T,?> supergraph, ExplodedSupergraphPath<T> path) {
    pruneListForCallReturn(supergraph, path.outermostList);
    for (Iterator<List<ExplodedSupergraphNode<T>>> it = path.edge2SLVP.values().iterator(); it.hasNext();) {
      List<ExplodedSupergraphNode<T>> l = it.next();
      pruneListForCallReturn(supergraph, l);
    }
  }

  /**
   * Create a brief(er) summary of a path, which includes only call and return
   * nodes
   */
  public static <T> List<ExplodedSupergraphNode<T>> pruneListForCallReturn(ISupergraph<T,?> supergraph, List<ExplodedSupergraphNode<T>> L) {
    for (int i = 0; i < L.size(); i++) {
      ExplodedSupergraphNode<T> n = L.get(i);
      if (!supergraph.isEntry(n.getSupergraphNode()) && !supergraph.isExit(n.getSupergraphNode())
          && !supergraph.isCall(n.getSupergraphNode()) && !supergraph.isReturn(n.getSupergraphNode())) {
        L.remove(i);
        i--;
      }
    }
    return L;
  }
}
