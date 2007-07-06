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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.BytecodeConstants;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.IntFunction;
import com.ibm.wala.util.IntMapIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSharedBitVectorIntSet;
import com.ibm.wala.util.intset.SparseIntSet;
import com.ibm.wala.util.intset.SparseVector;

/**
 * 
 * A call graph which explicitly holds the target for each call site in each
 * node.
 * 
 * 
 * @author sfink
 */
public class ExplicitCallGraph extends BasicCallGraph implements BytecodeConstants {

  private static final boolean DEBUG = false;

  /**
   * Governing class hierarchy
   */
  protected final IClassHierarchy cha;

  /**
   * Analysis options
   */
  protected final AnalysisOptions options;

  /**
   * special object to track call graph edges
   */
  private final ExplicitEdgeManager edgeManager = makeEdgeManger();

  public ExplicitCallGraph(IClassHierarchy cha, AnalysisOptions options) {
    super();
    this.cha = cha;
    this.options = options;
  }

  /**
   * subclasses may wish to override!
   */
  protected ExplicitNode makeNode(IMethod method, Context context) {
    return new ExplicitNode(method, context);
  }

  /**
   * subclasses may wish to override!
   */
  @Override
  protected CGNode makeFakeRootNode() {
    return findOrCreateNode(new FakeRootMethod(cha, options), Everywhere.EVERYWHERE);
  }
  
  /**
   * subclasses may wish to override!
   */
  @Override
  protected CGNode makeFakeWorldClinitNode() {
    return findOrCreateNode(new FakeWorldClinitMethod(cha, options), Everywhere.EVERYWHERE);
  }

  /**
   * Method findOrCreateNode.
   * 
   * @param method
   * @return NodeImpl
   */
  @Override
  public CGNode findOrCreateNode(IMethod method, Context C) {
    if (Assertions.verifyAssertions) {
      if (method == null || C == null) {
        Assertions._assert(method != null, "null method");
        Assertions._assert(C != null, "null context for method " + method);
      }
    }
    Key k = new Key(method, C);
    NodeImpl result = getNode(k);
    if (result == null) {
      result = makeNode(method, C);
      if (DEBUG) {
        Trace.println("Create node for " + method + "hash code " + method.hashCode());
      }
      registerNode(k, result);
    }
    return result;
  }

  public class ExplicitNode extends NodeImpl {

    /**
     * A Mapping from call site program counter (int) -> Object, where Object is
     * a CGNode if we've discovered exactly one target for the site, or an
     * IntSet of node numbers if we've discovered more than one target for the
     * site.
     */
    protected final SparseVector<Object> targets = new SparseVector<Object>();

    private final MutableSharedBitVectorIntSet allTargets = new MutableSharedBitVectorIntSet();

    /**
     * @param method
     */
    protected ExplicitNode(IMethod method, Context C) {
      super(method, C);
    }

    @Override
    public Set<CGNode> getPossibleTargets(CallSiteReference site) {
      Object result = targets.get(site.getProgramCounter());

      if (result == null) {
        return Collections.emptySet();
      } else if (result instanceof CGNode) {
        Set<CGNode> s = Collections.singleton((CGNode)result);
        return s;
      } else {
        IntSet s = (IntSet) result;
        HashSet<CGNode> h = HashSetFactory.make(s.size());
        for (IntIterator it = s.intIterator(); it.hasNext();) {
          h.add(getCallGraph().getNode(it.next()));
        }
        return h;
      }
    }

    public IntSet getPossibleTargetNumbers(CallSiteReference site) {
      Object t = targets.get(site.getProgramCounter());

      if (t == null) {
        return null;
      } else if (t instanceof CGNode) {
        return SparseIntSet.singleton(getCallGraph().getNumber((CGNode) t));
      } else {
        return (IntSet) t;
      }
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.CGNode#getPossibleSites(com.ibm.wala.ipa.callgraph.CGNode)
     */
    public Iterator<CallSiteReference> getPossibleSites(final CGNode to) {
      final int n = getCallGraph().getNumber(to);
      return new FilterIterator<CallSiteReference>(iterateSites(), new Filter() {
        public boolean accepts(Object o) {
          IntSet s = getPossibleTargetNumbers((CallSiteReference) o);
          return s == null ? false : s.contains(n);
        }
      });
    }

    @Override
    public boolean addTarget(CallSiteReference site, CGNode tNode) {
      return addTarget(site.getProgramCounter(), tNode);
    }

    protected boolean addTarget(int pc, CGNode tNode) {
      allTargets.add(getCallGraph().getNumber(tNode));
      Object S = targets.get(pc);
      if (S == null) {
        S = tNode;
        targets.set(pc, S);
        getCallGraph().addEdge(this, tNode);
        return true;
      } else {
        if (S instanceof CGNode) {
          if (S.equals(tNode)) {
            return false;
          } else {
            MutableSharedBitVectorIntSet s = new MutableSharedBitVectorIntSet();
            s.add(getCallGraph().getNumber((CGNode) S));
            s.add(getCallGraph().getNumber(tNode));
            getCallGraph().addEdge(this, tNode);
            targets.set(pc, s);
            return true;
          }
        } else {
          MutableIntSet s = (MutableIntSet) S;
          int n = getCallGraph().getNumber(tNode);
          if (!s.contains(n)) {
            s.add(n);
            getCallGraph().addEdge(this, tNode);
            return true;
          } else {
            return false;
          }
        }
      }
    }

    /*
     * @see com.ibm.detox.ipa.callgraph.CGNode#getNumberOfTargets(com.ibm.wala.classLoader.CallSiteReference)
     */
    public int getNumberOfTargets(CallSiteReference site) {
      Object result = targets.get(site.getProgramCounter());

      if (result == null) {
        return 0;
      } else if (result instanceof CGNode) {
        return 1;
      } else {
        return ((IntSet) result).size();
      }
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.CGNode#iterateSites()
     */
    public Iterator<CallSiteReference> iterateSites() {
      return getCallGraph().getInterpreter(this).iterateCallSites(this);
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.impl.BasicCallGraph.NodeImpl#removeTarget(com.ibm.wala.ipa.callgraph.CGNode)
     */
    public void removeTarget(CGNode target) {
      allTargets.remove(getCallGraph().getNumber(target));
      for (IntIterator it = targets.safeIterateIndices(); it.hasNext();) {
        int pc = it.next();
        Object value = targets.get(pc);
        if (value instanceof CGNode) {
          if (value.equals(target)) {
            targets.remove(pc);
          }
        } else {
          MutableIntSet s = (MutableIntSet) value;
          int n = getCallGraph().getNumber(target);
          if (s.size() > 2) {
            s.remove(n);
          } else {
            if (Assertions.verifyAssertions) {
              Assertions._assert(s.size() == 2);
            }
            if (s.contains(n)) {
              s.remove(n);
              int i = s.intIterator().next();
              targets.set(pc, getCallGraph().getNode(i));
            }
          }
        }
      }
    }

    @Override
    public boolean equals(Object obj) {
      // we can use object equality since these objects are canonical as created
      // by the governing ExplicitCallGraph
      return this == obj;
    }

    @Override
    public int hashCode() {
      // TODO: cache?
      return getMethod().hashCode() * 8681 + getContext().hashCode();
    }

    public MutableSharedBitVectorIntSet getAllTargetNumbers() {
      return allTargets;
    }

    public void clearAllTargets() {
      targets.clear();
      allTargets.clear();
    }

    public IR getIR() {
      return getCallGraph().getInterpreter(this).getIR(this);
    }
    
    public DefUse getDU() {
      return getCallGraph().getInterpreter(this).getDU(this);
    }

    public ExplicitCallGraph getCallGraph() {
      return ExplicitCallGraph.this;
    }

    public Iterator<CallSiteReference> iterateCallSites() {
      return getCallGraph().getInterpreter(this).iterateCallSites(this);
    }

    public Iterator<NewSiteReference> iterateNewSites() {
      return getCallGraph().getInterpreter(this).iterateNewSites(this);
    }

    public ControlFlowGraph getCFG() {
      return getCallGraph().getInterpreter(this).getCFG(this);
    }
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.CallGraph#getClassHierarchy()
   */
  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

  protected class ExplicitEdgeManager implements NumberedEdgeManager<CGNode> {

    final IntFunction<CGNode> toNode = new IntFunction<CGNode>() {
      public CGNode apply(int i) {
        CGNode result = getNode(i);
        // if (Assertions.verifyAssertions && result == null) {
        // Assertions.UNREACHABLE("uh oh " + i);
        // }
        return result;
      }
    };

    /**
     * for each y, the {x | (x,y) is an edge)
     */
    final IBinaryNaturalRelation predecessors = new BasicNaturalRelation(
        new byte[] { BasicNaturalRelation.SIMPLE_SPACE_STINGY }, BasicNaturalRelation.SIMPLE);

    public IntSet getSuccNodeNumbers(CGNode node) {
      ExplicitNode n = (ExplicitNode) node;
      return n.getAllTargetNumbers();
    }

    public IntSet getPredNodeNumbers(CGNode node) {
      ExplicitNode n = (ExplicitNode) node;
      int y = getNumber(n);
      return predecessors.getRelated(y);
    }

    public Iterator<CGNode> getPredNodes(CGNode N) {
      IntSet s = getPredNodeNumbers(N);
      if (s == null) {
        return EmptyIterator.instance();
      } else {
        return new IntMapIterator<CGNode>(s.intIterator(), toNode);
      }
    }

    public int getPredNodeCount(CGNode N) {
      ExplicitNode n = (ExplicitNode) N;
      int y = getNumber(n);
      return predecessors.getRelatedCount(y);
    }

    public Iterator<CGNode> getSuccNodes(CGNode N) {
      ExplicitNode n = (ExplicitNode) N;
      return new IntMapIterator<CGNode>(n.getAllTargetNumbers().intIterator(), toNode);
    }

    public int getSuccNodeCount(CGNode N) {
      ExplicitNode n = (ExplicitNode) N;
      return n.getAllTargetNumbers().size();
    }

    public void addEdge(CGNode src, CGNode dst) {
      // we assume that this is called from ExplicitNode.addTarget().
      // so we only have to track the inverse edge.
      int x = getNumber(src);
      int y = getNumber(dst);
      predecessors.add(y, x);
    }

    public void removeEdge(CGNode src, CGNode dst) {
      int x = getNumber(src);
      int y = getNumber(dst);
      predecessors.remove(y, x);
    }

    protected void addEdge(int x, int y) {
      // we only have to track the inverse edge.
      predecessors.add(y, x);
    }

    public void removeAllIncidentEdges(CGNode node) {
      Assertions.UNREACHABLE();
    }

    public void removeIncomingEdges(CGNode node) {
      Assertions.UNREACHABLE();

    }

    public void removeOutgoingEdges(CGNode node) {
      Assertions.UNREACHABLE();

    }

    public boolean hasEdge(CGNode src, CGNode dst) {
      int x = getNumber(src);
      int y = getNumber(dst);
      return predecessors.contains(y, x);
    }
  }

  /**
   * @return Returns the edgeManger.
   */
  @Override
  public EdgeManager<CGNode> getEdgeManager() {
    return edgeManager;
  }

  protected ExplicitEdgeManager makeEdgeManger() {
    return new ExplicitEdgeManager();
  }
}
