package com.ibm.wala.ipa.callgraph.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.impl.DelegatingGraph;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

public class PartialCallGraph extends DelegatingGraph<CGNode> implements CallGraph {

  private final CallGraph cg;

  private final Set<CGNode> partialRoots;

  private PartialCallGraph(CallGraph cg, Set<CGNode> partialRoots, Graph<CGNode> partialGraph) {
    super(partialGraph);
    this.cg = cg;
    this.partialRoots = partialRoots;
  }

  public static PartialCallGraph make(CallGraph CG, Set<CGNode> partialRoots) {
    final Set<CGNode> nodes = DFS.getReachableNodes(CG, partialRoots);
    Graph<CGNode> partialGraph = GraphSlicer.prune(CG, new Filter() {
      public boolean accepts(Object o) {
        return nodes.contains(o);
      }
    });

    return new PartialCallGraph(CG, partialRoots, partialGraph);
  }

  public CGNode getFakeRootNode() {
    Assertions.UNREACHABLE();
    return null;
  }

  public Collection<CGNode> getEntrypointNodes() {
    return partialRoots;
  }

  public CGNode getNode(IMethod method, Context C) {
    CGNode x = cg.getNode(method, C);
    if (containsNode(x)) {
      return x;
    } else {
      return null;
    }
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

  public void dump(String filename) {
    Assertions.UNREACHABLE();
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
    Assertions._assert(containsNode(n));
    return n;
  }

  public int getNumber(CGNode n) {
    Assertions._assert(containsNode(n));
    return cg.getNumber(n);
  }

  public IntSet getSuccNodeNumbers(CGNode node) {
    Assertions._assert(containsNode(node));
    MutableIntSet x = IntSetUtil.make();
    for (Iterator ns = getSuccNodes(node); ns.hasNext();) {
      x.add(getNumber((CGNode) ns.next()));
    }

    return x;
  }

  public IntSet getPredNodeNumbers(CGNode node) {
    Assertions._assert(containsNode(node));
    MutableIntSet x = IntSetUtil.make();
    for (Iterator ns = getPredNodes(node); ns.hasNext();) {
      x.add(getNumber((CGNode) ns.next()));
    }

    return x;
  }
}
