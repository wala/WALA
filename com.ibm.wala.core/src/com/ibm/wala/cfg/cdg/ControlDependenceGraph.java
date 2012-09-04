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
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.dominators.DominanceFrontiers;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * Control Dependence Graph
 */
public class ControlDependenceGraph<I, T extends IBasicBlock<I>> extends AbstractNumberedGraph<T> {

  /**
   * Governing control flow-graph. The control dependence graph is computed from this cfg.
   */
  private final ControlFlowGraph<I, T> cfg;

  /**
   * the EdgeManager for the CDG. It implements the edge part of the standard Graph abstraction, using the control-dependence edges
   * of the cdg.
   */
  private final NumberedEdgeManager<T> edgeManager;

  /**
   * If requested, this is a map from parentXchild Pairs representing edges in the CDG to the labels of the control flow edges that
   * edge corresponds to. The labels are Boolean.True or Boolean.False for conditionals and an Integer for a switch label.
   */
  private Map<Pair, Set<Object>> edgeLabels;

  /**
   * This is the heart of the CDG computation. Based on Cytron et al., this is the reverse dominance frontier based algorithm for
   * computing control dependence edges.
   * 
   * @return Map: node n -> {x : n is control-dependent on x}
   */
  private Map<T, Set<T>> buildControlDependence(boolean wantEdgeLabels) {
    Map<T, Set<T>> controlDependence = HashMapFactory.make(cfg.getNumberOfNodes());

    DominanceFrontiers<T> RDF = new DominanceFrontiers<T>(GraphInverter.invert(cfg), cfg.exit());

    if (wantEdgeLabels) {
      edgeLabels = HashMapFactory.make();
    }

    for (Iterator<? extends T> ns = cfg.iterator(); ns.hasNext();) {
      HashSet<T> s = HashSetFactory.make(2);
      controlDependence.put(ns.next(), s);
    }

    for (Iterator<? extends T> ns = cfg.iterator(); ns.hasNext();) {
      T y = ns.next();
      for (Iterator<T> ns2 = RDF.getDominanceFrontier(y); ns2.hasNext();) {
        T x = ns2.next();
        controlDependence.get(x).add(y);
        if (wantEdgeLabels) {
          Set<Object> labels = HashSetFactory.make();
          edgeLabels.put(Pair.make(x, y), labels);
          for (Iterator<? extends T> ss = cfg.getSuccNodes(x); ss.hasNext();) {
            T s = ss.next();
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
   * Given the control-dependence edges in a forward direction (i.e. edges from control parents to control children), this method
   * creates an EdgeManager that provides the edge half of the Graph abstraction.
   */
  private NumberedEdgeManager<T> constructGraphEdges(final Map<T, Set<T>> forwardEdges) {
    return new NumberedEdgeManager<T>() {
      Map<T, Set<T>> backwardEdges = HashMapFactory.make(forwardEdges.size());
      {
        for (Iterator<? extends T> x = cfg.iterator(); x.hasNext();) {
          Set<T> s = HashSetFactory.make();
          backwardEdges.put(x.next(), s);
        }
        for (Iterator<T> ps = forwardEdges.keySet().iterator(); ps.hasNext();) {
          T p = ps.next();
          for (Iterator ns = ((Set) forwardEdges.get(p)).iterator(); ns.hasNext();) {
            Object n = ns.next();
            backwardEdges.get(n).add(p);
          }
        }
      }

      public Iterator<T> getPredNodes(T N) {
        if (backwardEdges.containsKey(N))
          return backwardEdges.get(N).iterator();
        else
          return EmptyIterator.instance();
      }

      public IntSet getPredNodeNumbers(T node) {
        MutableIntSet x = IntSetUtil.make();
        if (backwardEdges.containsKey(node)) {
          for(T pred : backwardEdges.get(node)) {
            x.add(pred.getNumber());
          }
        }
        return x;
      }

      public int getPredNodeCount(T N) {
        if (backwardEdges.containsKey(N))
          return ((Set) backwardEdges.get(N)).size();
        else
          return 0;
      }

      public Iterator<T> getSuccNodes(T N) {
        if (forwardEdges.containsKey(N))
          return forwardEdges.get(N).iterator();
        else
          return EmptyIterator.instance();
      }

      public IntSet getSuccNodeNumbers(T node) {
        MutableIntSet x = IntSetUtil.make();
        if (forwardEdges.containsKey(node)) {
          for(T succ : forwardEdges.get(node)) {
            x.add(succ.getNumber());
          }
        }
        return x;
      }

      public int getSuccNodeCount(T N) {
        if (forwardEdges.containsKey(N))
          return ((Set) forwardEdges.get(N)).size();
        else
          return 0;
      }

      public boolean hasEdge(T src, T dst) {
        return forwardEdges.containsKey(src) && ((Set) forwardEdges.get(src)).contains(dst);
      }

      public void addEdge(T src, T dst) {
        throw new UnsupportedOperationException();
      }

      public void removeEdge(T src, T dst) {
        throw new UnsupportedOperationException();
      }

      public void removeAllIncidentEdges(T node) {
        throw new UnsupportedOperationException();
      }

      public void removeIncomingEdges(T node) {
        throw new UnsupportedOperationException();
      }

      public void removeOutgoingEdges(T node) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (Iterator<? extends T> ns = iterator(); ns.hasNext();) {
      T n = ns.next();
      sb.append(n.toString()).append("\n");
      for (Iterator ss = getSuccNodes(n); ss.hasNext();) {
        Object s = ss.next();
        sb.append("  --> ").append(s);
        if (edgeLabels != null)
          for (Iterator labels = ((Set) edgeLabels.get(Pair.make(n, s))).iterator(); labels.hasNext();)
            sb.append("\n   label: ").append(labels.next());
        sb.append("\n");
      }
    }

    return sb.toString();
  }

  /**
   * @param cfg governing control flow graph
   * @param wantEdgeLabels whether to compute edge labels for CDG edges
   */
  public ControlDependenceGraph(ControlFlowGraph<I, T> cfg, boolean wantEdgeLabels) {
    if (cfg == null) {
      throw new IllegalArgumentException("null cfg");
    }
    this.cfg = cfg;
    this.edgeManager = constructGraphEdges(buildControlDependence(wantEdgeLabels));
  }

  /**
   * @param cfg governing control flow graph
   */
  public ControlDependenceGraph(ControlFlowGraph<I, T> cfg) {
    this(cfg, false);
  }

  public ControlFlowGraph getControlFlowGraph() {
    return cfg;
  }

  /**
   * Return the set of edge labels for the control flow edges that cause the given edge in the CDG. Requires that the CDG be
   * constructed with wantEdgeLabels being true.
   */
  public Set<Object> getEdgeLabels(Object from, Object to) {
    return edgeLabels.get(Pair.make(from, to));
  }

  @Override
  public NumberedNodeManager<T> getNodeManager() {
    return cfg;
  }

  @Override
  public NumberedEdgeManager<T> getEdgeManager() {
    return edgeManager;
  }

  public boolean controlEquivalent(T bb1, T bb2) {
    if (getPredNodeCount(bb1) != getPredNodeCount(bb2)) {
      return false;
    }

    for (Iterator<? extends T> pbs1 = getPredNodes(bb1); pbs1.hasNext();) {
      if (!hasEdge(pbs1.next(), bb2)) {
        return false;
      }
    }

    return true;
  }
}
