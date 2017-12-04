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

import com.ibm.wala.cfg.MinimalCFG;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
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
public class ControlDependenceGraph<T> extends AbstractNumberedGraph<T> {

  /**
   * Governing control flow-graph. The control dependence graph is computed from this cfg.
   */
  private final MinimalCFG<T> cfg;

  /**
   * the EdgeManager for the CDG. It implements the edge part of the standard Graph abstraction, using the control-dependence edges
   * of the cdg.
   */
  private final NumberedEdgeManager<T> edgeManager;

  /**
   * If requested, this is a map from parentXchild Pairs representing edges in the CDG to the labels of the control flow edges that
   * edge corresponds to. The labels are Boolean.True or Boolean.False for conditionals and an Integer for a switch label.
   */
  private Map<Pair<T,T>, Set<? extends Object>> edgeLabels;

  /**
   * This is the heart of the CDG computation. Based on Cytron et al., this is the reverse dominance frontier based algorithm for
   * computing control dependence edges.
   * 
   * @return Map: node n -&gt; {x : n is control-dependent on x}
   */
  private Map<T, Set<T>> buildControlDependence(boolean wantEdgeLabels) {
    Map<T, Set<T>> controlDependence = HashMapFactory.make(cfg.getNumberOfNodes());

    DominanceFrontiers<T> RDF = new DominanceFrontiers<>(GraphInverter.invert(cfg), cfg.exit());

    if (wantEdgeLabels) {
      edgeLabels = HashMapFactory.make();
    }

    for (T name : cfg) {
      HashSet<T> s = HashSetFactory.make(2);
      controlDependence.put(name, s);
    }

    for (T y : cfg) {
      for (T x : Iterator2Iterable.make(RDF.getDominanceFrontier(y))) {
        controlDependence.get(x).add(y);
        if (wantEdgeLabels) {
           HashSet<Object> labels = HashSetFactory.make();
          edgeLabels.put(Pair.make(x, y), labels);
          for (T s : Iterator2Iterable.make(cfg.getSuccNodes(x))) {
            if (RDF.isDominatedBy(s, y)) {
              labels.add(makeEdgeLabel(x, y, s));
            }
          }
        }
      }
    }

    return controlDependence;
  }

  protected Object makeEdgeLabel(@SuppressWarnings("unused") T from, @SuppressWarnings("unused") T to, T s) {
    return s;
  }  

   /**
   * Given the control-dependence edges in a forward direction (i.e. edges from control parents to control children), this method
   * creates an EdgeManager that provides the edge half of the Graph abstraction.
   */
  private NumberedEdgeManager<T> constructGraphEdges(final Map<T, Set<T>> forwardEdges) {
    return new NumberedEdgeManager<T>() {
      Map<T, Set<T>> backwardEdges = HashMapFactory.make(forwardEdges.size());
      {
        for (T name : cfg) {
          Set<T> s = HashSetFactory.make();
          backwardEdges.put(name, s);
        }
        for (T p : forwardEdges.keySet()) {
          for (T t : forwardEdges.get(p)) {
            Object n = t;
            backwardEdges.get(n).add(p);
          }
        }
      }

      @Override
      public Iterator<T> getPredNodes(T N) {
        if (backwardEdges.containsKey(N))
          return backwardEdges.get(N).iterator();
        else
          return EmptyIterator.instance();
      }

      @Override
      public IntSet getPredNodeNumbers(T node) {
        MutableIntSet x = IntSetUtil.make();
        if (backwardEdges.containsKey(node)) {
          for(T pred : backwardEdges.get(node)) {
            x.add(cfg.getNumber(pred));
          }
        }
        return x;
      }

      @Override
      public int getPredNodeCount(T N) {
        if (backwardEdges.containsKey(N))
          return ((Set) backwardEdges.get(N)).size();
        else
          return 0;
      }

      @Override
      public Iterator<T> getSuccNodes(T N) {
        if (forwardEdges.containsKey(N))
          return forwardEdges.get(N).iterator();
        else
          return EmptyIterator.instance();
      }

      @Override
      public IntSet getSuccNodeNumbers(T node) {
        MutableIntSet x = IntSetUtil.make();
        if (forwardEdges.containsKey(node)) {
          for(T succ : forwardEdges.get(node)) {
            x.add(cfg.getNumber(succ));
          }
        }
        return x;
      }

      @Override
      public int getSuccNodeCount(T N) {
        if (forwardEdges.containsKey(N))
          return ((Set) forwardEdges.get(N)).size();
        else
          return 0;
      }

      @Override
      public boolean hasEdge(T src, T dst) {
        return forwardEdges.containsKey(src) && ((Set) forwardEdges.get(src)).contains(dst);
      }

      @Override
      public void addEdge(T src, T dst) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void removeEdge(T src, T dst) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void removeAllIncidentEdges(T node) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void removeIncomingEdges(T node) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void removeOutgoingEdges(T node) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (T n : this) {
      sb.append(n.toString()).append("\n");
      for (T s : Iterator2Iterable.make(getSuccNodes(n))) {
        sb.append("  --> ").append(s);
        if (edgeLabels != null)
          for (Object name : edgeLabels.get(Pair.make(n, s)))
            sb.append("\n   label: ").append(name);
        sb.append("\n");
      }
    }

    return sb.toString();
  }

  /**
   * @param cfg governing control flow graph
   * @param wantEdgeLabels whether to compute edge labels for CDG edges
   */
  public ControlDependenceGraph(MinimalCFG<T> cfg, boolean wantEdgeLabels) {
    if (cfg == null) {
      throw new IllegalArgumentException("null cfg");
    }
    this.cfg = cfg;
    this.edgeManager = constructGraphEdges(buildControlDependence(wantEdgeLabels));
  }

  /**
   * @param cfg governing control flow graph
   */
  public ControlDependenceGraph(MinimalCFG<T> cfg) {
    this(cfg, false);
  }

  public MinimalCFG getControlFlowGraph() {
    return cfg;
  }

  /**
   * Return the set of edge labels for the control flow edges that cause the given edge in the CDG. Requires that the CDG be
   * constructed with wantEdgeLabels being true.
   */
  public Set<? extends Object> getEdgeLabels(T from, T to) {
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

    for (T pb : Iterator2Iterable.make(getPredNodes(bb1))) {
      if (!hasEdge(pb, bb2)) {
        return false;
      }
    }

    return true;
  }
}
