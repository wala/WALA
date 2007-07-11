package com.ibm.wala.core.ipa.callgraph.impl;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.*;
import com.ibm.wala.util.graph.*;
import com.ibm.wala.util.graph.impl.*;
import com.ibm.wala.util.graph.traverse.*;
import com.ibm.wala.util.intset.*;

import java.util.*;

public class PartialCallGraph extends DelegatingGraph<CGNode>
    implements CallGraph
{

  private final CallGraph CG;

  private final Set partialRoots;

  private PartialCallGraph(CallGraph CG, Set partialRoots, Graph partialGraph) {
    super(partialGraph);
    this.CG = CG;
    this.partialRoots = partialRoots;
  }
    
  public static PartialCallGraph make(CallGraph CG, Set partialRoots) {
    final Set nodes = DFS.getReachableNodes(CG, partialRoots);
    Graph partialGraph = GraphSlicer.prune(CG, new Filter() {
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
    CGNode x = CG.getNode(method, C);
    if (containsNode(x)) {
      return x;
    } else {
      return null;
    }
  }

  public Set<CGNode> getNodes(MethodReference m) {
    Set result = new HashSet();
    for(Iterator xs = CG.getNodes(m).iterator(); xs.hasNext(); ) {
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
    return CG.getClassHierarchy();
  }

  public Iterator iterateNodes(IntSet nodes) {
    return new FilterIterator(CG.iterateNodes(nodes),
      new Filter() {
        public boolean accepts(Object o) {
	  return containsNode((CGNode)o);
	}
    });
  }

  public int getMaxNumber() {
    return CG.getMaxNumber();
  }

  public CGNode getNode(int index) {
    CGNode n = CG.getNode(index);
    Assertions._assert(containsNode(n));
    return n;
  }

  public int getNumber(CGNode n) {
    Assertions._assert(containsNode(n));
    return CG.getNumber(n);
  }

  public IntSet getSuccNodeNumbers(CGNode node) {
    Assertions._assert(containsNode(node));
    MutableIntSet x = IntSetUtil.make();
    for(Iterator ns = getSuccNodes(node); ns.hasNext(); ) {
      x.add( getNumber((CGNode)ns.next()) );
    }

    return x;
  }

  public IntSet getPredNodeNumbers(CGNode node) {
    Assertions._assert(containsNode(node));
    MutableIntSet x = IntSetUtil.make();
    for(Iterator ns = getPredNodes(node); ns.hasNext(); ) {
      x.add( getNumber((CGNode)ns.next()) );
    }

    return x;
  }
}

    
