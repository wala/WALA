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
package com.ibm.wala.ipa.callgraph.propagation.rta;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSharedBitVectorIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * A call graph implementation where some edges are delegated to other other
 * call sites, since they are guaranteed to be the same.
 * 
 * @author sfink
 * 
 */
public class DelegatingExplicitCallGraph extends ExplicitCallGraph {

  /**
   * delegateR(x,y) means that for at least one site, node number y delegates to
   * node number x.
   */
  private final IBinaryNaturalRelation delegateR = new BasicNaturalRelation();

  /**
   * @param cha
   * @param options
   */
  public DelegatingExplicitCallGraph(ClassHierarchy cha, AnalysisOptions options) {
    super(cha, options);
  }

  /**
   * In this implementation, super.targets is a mapping from call site ->
   * Object, where Object is a
   * <ul>
   * A Mapping from call site -> Object, where Object is a
   * <li>CGNode if we've discovered exactly one target for the site
   * <li> or an IntSet of node numbers if we've discovered more than one target
   * for the site.
   * <li> a CallSite if we're delegating these edges to another node
   * </ul>
   */
  public class DelegatingCGNode extends ExplicitNode {

    protected DelegatingCGNode(IMethod method, Context C) {
      super(method, C);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitNode#getAllTargetNumbers()
     */
    public MutableSharedBitVectorIntSet getAllTargetNumbers() {
      MutableSharedBitVectorIntSet result = new MutableSharedBitVectorIntSet(super.getAllTargetNumbers());
      for (Iterator it = targets.iterator(); it.hasNext();) {
        Object n = it.next();
        if (n instanceof CallSite) {
          ExplicitNode delegate = (ExplicitNode) ((CallSite) n).getNode();
          IntSet s = delegate.getPossibleTargetNumbers(((CallSite) n).getSite());
          if (s != null) {
            result.addAll(s);
          }
        }
      }
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ipa.callgraph.CGNode#getPossibleTargets(com.ibm.wala.classLoader.CallSiteReference)
     */
    public Set<CGNode> getPossibleTargets(CallSiteReference site) {
      Object result = targets.get(site.getProgramCounter());
      if (result != null && result instanceof CallSite) {
        CallSite p = (CallSite) result;
        CGNode n = p.getNode();
        CallSiteReference s = p.getSite();
        return n.getPossibleTargets(s);
      } else {
        return super.getPossibleTargets(site);
      }
    }

    public IntSet getPossibleTargetNumbers(CallSiteReference site) {
      Object t = targets.get(site.getProgramCounter());
      if (t != null && t instanceof CallSite) {
        CallSite p = (CallSite) t;
        DelegatingCGNode n = (DelegatingCGNode) p.getNode();
        CallSiteReference s = p.getSite();
        return n.getPossibleTargetNumbers(s);
      } else {
        return super.getPossibleTargetNumbers(site);
      }
    }

    private boolean hasTarget(int y) {
      if (super.getAllTargetNumbers().contains(y)) {
        return true;
      } else {
        for (Iterator it = targets.iterator(); it.hasNext();) {
          Object n = it.next();
          if (n instanceof CallSite) {
            ExplicitNode delegate = (ExplicitNode) ((CallSite) n).getNode();
            IntSet s = delegate.getPossibleTargetNumbers(((CallSite) n).getSite());
            if (s!= null && s.contains(y)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.detox.ipa.callgraph.CGNode#getNumberOfTargets(com.ibm.wala.classLoader.CallSiteReference)
     */
    public int getNumberOfTargets(CallSiteReference site) {
      Object result = targets.get(site.getProgramCounter());
      if (result != null && result instanceof CallSite) {
        CallSite p = (CallSite) result;
        CGNode n = p.getNode();
        CallSiteReference s = p.getSite();
        return n.getNumberOfTargets(s);
      } else {
        return super.getNumberOfTargets(site);
      }
    }

    public void delegate(CallSiteReference site, CGNode delegateNode, CallSiteReference delegateSite) {
      CallSite d = new CallSite(delegateSite, delegateNode);
      targets.set(site.getProgramCounter(), d);
      int y = getNumber(this);
      int x = getNumber(delegateNode);
      delegateR.add(x, y);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph#makeNode(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.Context)
   */
  protected ExplicitNode makeNode(IMethod method, Context context) {
    return new DelegatingCGNode(method, context);
  }

  private class DelegatingEdgeManager extends ExplicitEdgeManager {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitEdgeManager#addEdge(java.lang.Object,
     *      java.lang.Object)
     */
    public void addEdge(CGNode src, CGNode dst) {
      // we assume that this is called from ExplicitNode.addTarget().
      // so we only have to track the inverse edge.
      // this structure is pretty fragile .. we only explicitly represent the
      // edges when NOT delegating.
      // see getPredNodeNumbers() below which recovers.
      super.addEdge(src, dst);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitEdgeManager#removeAllIncidentEdges(java.lang.Object)
     */
    public void removeAllIncidentEdges(CGNode node) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitEdgeManager#removeIncomingEdges(java.lang.Object)
     */
    public void removeIncomingEdges(CGNode node) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitEdgeManager#removeOutgoingEdges(java.lang.Object)
     */
    public void removeOutgoingEdges(CGNode node) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitEdgeManager#hasEdge(java.lang.Object,
     *      java.lang.Object)
     */
    public boolean hasEdge(CGNode src, CGNode dst) {
      if (super.hasEdge(src, dst)) {
        return true;
      } else {
        DelegatingCGNode s = (DelegatingCGNode) src;
        int y = getNumber(dst);
        return s.hasTarget(y);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitEdgeManager#getPredNodeCount(java.lang.Object)
     */
    public int getPredNodeCount(CGNode N) {
      IntSet s = getPredNodeNumbers(N);
      return s == null ? 0 : s.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitEdgeManager#getPredNodeNumbers(java.lang.Object)
     */
    public IntSet getPredNodeNumbers(CGNode node) {
      IntSet superR = super.getPredNodeNumbers(node);
      if (superR == null) {
        return null;
      } else {
        MutableSparseIntSet result = new MutableSparseIntSet(superR);
        BitVectorIntSet allPossiblePreds = new BitVectorIntSet(superR);
        for (IntIterator it = superR.intIterator(); it.hasNext();) {
          int x = it.next();
          IntSet ySet = delegateR.getRelated(x);
          if (ySet != null) {
            allPossiblePreds.addAll(ySet);
          }
        }
        for (IntIterator it = allPossiblePreds.intIterator(); it.hasNext();) {
          int y = it.next();
          DelegatingCGNode yNode = (DelegatingCGNode) getNode(y);
          if (hasEdge(yNode, node)) {
            result.add(y);
          }
        }
        return result;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph#makeEdgeManger()
   */
  protected ExplicitEdgeManager makeEdgeManger() {
    return new DelegatingEdgeManager();
  };

}
