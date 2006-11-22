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
package com.ibm.wala.cfg.cdg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.DominanceFrontiers;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.impl.GraphInverter;

/**
 * Control Dependence Graph
 * 
 * @author Julian Dolby
 * 
 */
public class ControlDependenceGraph extends AbstractNumberedGraph<IBasicBlock> {

  /**
   * Governing control flow-graph. The control dependence graph is computed from
   * this cfg.
   */
  private final ControlFlowGraph cfg;

  /**
   * the EdgeManager for the CDG. It implements the edge part of the standard
   * Graph abstraction, using the control-dependence egdes of the cdg.
   */
  private final EdgeManager<IBasicBlock> edgeManager;

  /**
   * If requested, this is a map from parentXchild Pairs representing edges in
   * the CDG to the labels of the control flow edges that edge corresponds to.
   * The labels are Boolean.True or Boolean.False for conditionals and an
   * Integer for a switch label.
   */
  private Map<Pair, Set<Object>> edgeLabels;

  /**
   * This is the heart of the CDG computation. Based on Cytron et al., this is
   * the reverse dominance frontier based algorithm for computing control
   * dependence edges.
   * 
   * @return Map: node n -> {x : n is control-dependent on x}
   */
  private Map<IBasicBlock, Set<IBasicBlock>> buildControlDependence(boolean wantEdgeLabels) {
    Map<IBasicBlock, Set<IBasicBlock>> controlDependence = new HashMap<IBasicBlock, Set<IBasicBlock>>(cfg.getNumberOfNodes());

    DominanceFrontiers<IBasicBlock> RDF = new DominanceFrontiers<IBasicBlock>(GraphInverter.invert(cfg), cfg.exit());

    if (wantEdgeLabels) {
      edgeLabels = HashMapFactory.make();
    }

    for (Iterator<? extends IBasicBlock> ns = cfg.iterateNodes(); ns.hasNext();) {
      controlDependence.put(ns.next(), new HashSet<IBasicBlock>(2));
    }

    for (Iterator<? extends IBasicBlock> ns = cfg.iterateNodes(); ns.hasNext();) {
      IBasicBlock y = ns.next();
      for (Iterator<IBasicBlock> ns2 = RDF.getDominanceFrontier(y); ns2.hasNext();) {
        IBasicBlock x = ns2.next();
        controlDependence.get(x).add(y);
        if (wantEdgeLabels) {
          Set<Object> labels = new HashSet<Object>();
          edgeLabels.put(new Pair<Object, Object>(x, y), labels);
          for (Iterator<? extends IBasicBlock> ss = cfg.getSuccNodes(x); ss.hasNext();) {
            IBasicBlock s = ss.next();
            if (RDF.isDominatedBy(s, y)) {
              labels.add(s);
            }
          }
        }
      }
    }

    return controlDependence;
  }

  /**
   * Given the control-dependence edges in a forward direction (i.e. edges from
   * control parents to control children), this method creates an EdgeManager
   * that provides the edge half of the Graph abstraction.
   */
  private EdgeManager<IBasicBlock> constructGraphEdges(final Map<IBasicBlock, Set<IBasicBlock>> forwardEdges) {
    return new EdgeManager<IBasicBlock>() {
      Map<IBasicBlock, Set<IBasicBlock>> backwardEdges = new HashMap<IBasicBlock, Set<IBasicBlock>>(forwardEdges.size());
      {
        for (Iterator<? extends IBasicBlock> x = cfg.iterateNodes(); x.hasNext();) {
          Set<IBasicBlock> s = HashSetFactory.make();
          backwardEdges.put(x.next(), s);
        }
        for (Iterator<IBasicBlock> ps = forwardEdges.keySet().iterator(); ps.hasNext();) {
          IBasicBlock p = ps.next();
          for (Iterator ns = ((Set) forwardEdges.get(p)).iterator(); ns.hasNext();) {
            Object n = ns.next();
            backwardEdges.get(n).add(p);
          }
        }
      }

      public Iterator<IBasicBlock> getPredNodes(IBasicBlock N) {
        if (backwardEdges.containsKey(N))
          return backwardEdges.get(N).iterator();
        else
          return EmptyIterator.instance();
      }

      public int getPredNodeCount(IBasicBlock N) {
        if (backwardEdges.containsKey(N))
          return ((Set) backwardEdges.get(N)).size();
        else
          return 0;
      }

      public Iterator<IBasicBlock> getSuccNodes(IBasicBlock N) {
        if (forwardEdges.containsKey(N))
          return forwardEdges.get(N).iterator();
        else
          return EmptyIterator.instance();
      }

      public int getSuccNodeCount(IBasicBlock N) {
        if (forwardEdges.containsKey(N))
          return ((Set) forwardEdges.get(N)).size();
        else
          return 0;
      }

      public boolean hasEdge(IBasicBlock src, IBasicBlock dst) {
        return forwardEdges.containsKey(src) && ((Set) forwardEdges.get(src)).contains(dst);
      }

      public void addEdge(IBasicBlock src, IBasicBlock dst) {
        throw new UnsupportedOperationException();
      }

      public void removeEdge(IBasicBlock src, IBasicBlock dst) {
        throw new UnsupportedOperationException();
      }

      public void removeAllIncidentEdges(IBasicBlock node) {
        throw new UnsupportedOperationException();
      }

      public void removeIncomingEdges(IBasicBlock node) {
        throw new UnsupportedOperationException();
      }

      public void removeOutgoingEdges(IBasicBlock node) {
        throw new UnsupportedOperationException();
      }
    };
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (Iterator<? extends IBasicBlock> ns = iterateNodes(); ns.hasNext();) {
      IBasicBlock n = ns.next();
      sb.append(n.toString()).append("\n");
      for (Iterator ss = getSuccNodes(n); ss.hasNext();) {
        Object s = ss.next();
        sb.append("  --> ").append(s);
        if (edgeLabels != null)
          for (Iterator labels = ((Set) edgeLabels.get(new Pair<IBasicBlock, Object>(n, s))).iterator(); labels.hasNext();)
            sb.append("\n   label: ").append(labels.next());
        sb.append("\n");
      }
    }

    return sb.toString();
  }

  /**
   * @param cfg
   *          governing control flow graph
   * @param wantEdgeLabels
   *          whether to compute edge labels for CDG edges
   */
  public ControlDependenceGraph(ControlFlowGraph cfg, boolean wantEdgeLabels) {
    this.cfg = cfg;
    this.edgeManager = constructGraphEdges(buildControlDependence(wantEdgeLabels));
  }

  /**
   * @param cfg
   *          governing control flow graph
   */
  public ControlDependenceGraph(ControlFlowGraph cfg) {
    this(cfg, false);
  }

  public ControlFlowGraph getUnderlyingCFG() {
    return cfg;
  }

  /**
   * Return the set of edge labels for the control flow edges that cause the
   * given edge in the CDG. Requires that the CDG be constructed with
   * wantEdgeLabels being true.
   */
  public Set<Object> getEdgeLabels(Object from, Object to) {
    return edgeLabels.get(new Pair<Object, Object>(from, to));
  }

  /*
   * (non-Javadoc)
   */
  public NodeManager<IBasicBlock> getNodeManager() {
    return cfg;
  }

  /*
   * (non-Javadoc)
   */
  public EdgeManager<IBasicBlock> getEdgeManager() {
    return edgeManager;
  }

  public boolean controlEquivalent(IBasicBlock bb1, IBasicBlock bb2) {
    if (getPredNodeCount(bb1) != getPredNodeCount(bb2)) {
      return false;
    }

    for (Iterator pbs1 = getPredNodes(bb1); pbs1.hasNext();) {
      if (!hasEdge((IBasicBlock) pbs1.next(), bb2)) {
        return false;
      }
    }

    return true;
  }

}
