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
package com.ibm.wala.util.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * Utilities related to simple graph subset operations.
 */
public class GraphSlicer {

  /**
   * Performs a backward slice.
   * 
   * @param <T> type for nodes
   * @param g the graph to slice
   * @param p identifies targets for the backward slice
   * @return the set of nodes in g, from which any of the targets (nodes that f accepts) is reachable.
   */
  public static <T> Set<T> slice(Graph<T> g, Predicate<T> p){
    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    HashSet<T> roots = HashSetFactory.make();
    for (T o : g) {
      if (p.test(o)) {
        roots.add(o);
      }
    }

    Set<T> result = DFS.getReachableNodes(GraphInverter.invert(g), roots);

    return result;
  }

  /**
   * Prune a graph to only the nodes accepted by the {@link Predicate} p
   */
  public static <T> Graph<T> prune(final Graph<T> g, final Predicate<T> p) {
    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    final NodeManager<T> n = new NodeManager<T>() {
      int nodeCount = -1;

      @Override
      public Iterator<T> iterator() {
        return new FilterIterator<>(g.iterator(), p);
      }

      @Override
      public int getNumberOfNodes() {
        if (nodeCount == -1) {
          nodeCount = IteratorUtil.count(iterator());
        }
        return nodeCount;
      }

      @Override
      public void addNode(T n) {
        Assertions.UNREACHABLE();
      }

      @Override
      public void removeNode(T n) {
        Assertions.UNREACHABLE();
      }

      @Override
      public boolean containsNode(T n) {
        return p.test(n) && g.containsNode(n);
      }

    };
    final EdgeManager<T> e = new EdgeManager<T>() {

      @Override
      public Iterator<T> getPredNodes(T n) {
        return new FilterIterator<>(g.getPredNodes(n), p);
      }

      @Override
      public int getPredNodeCount(T n) {
        return IteratorUtil.count(getPredNodes(n));
      }

      @Override
      public Iterator<T> getSuccNodes(T n) {
        return new FilterIterator<>(g.getSuccNodes(n), p);
      }

      @Override
      public int getSuccNodeCount(T N) {
        return IteratorUtil.count(getSuccNodes(N));
      }

      @Override
      public void addEdge(T src, T dst) {
        Assertions.UNREACHABLE();
      }

      @Override
      public void removeEdge(T src, T dst) {
        Assertions.UNREACHABLE();
      }

      @Override
      public void removeAllIncidentEdges(T node) {
        Assertions.UNREACHABLE();
      }

      @Override
      public void removeIncomingEdges(T node) {
        Assertions.UNREACHABLE();
      }

      @Override
      public void removeOutgoingEdges(T node) {
        Assertions.UNREACHABLE();
      }

      @Override
      public boolean hasEdge(T src, T dst) {
        return g.hasEdge(src, dst) && p.test(src) && p.test(dst);
      }

    };
    AbstractGraph<T> output = new AbstractGraph<T>() {

      @Override
      protected NodeManager<T> getNodeManager() {
        return n;
      }

      @Override
      protected EdgeManager<T> getEdgeManager() {
        return e;
      }

    };

    return output;
  }
  
  public static <E> AbstractGraph<E> project(final Graph<E> G, final Predicate<E> fmember) {
    final NodeManager<E> nodeManager = new NodeManager<E>() {
      private int count = -1;

      @Override
      public void addNode(E n) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsNode(E N) {
        return G.containsNode(N) && fmember.test(N);
      }

      @Override
      public int getNumberOfNodes() {
        if (count == -1) {
          count = IteratorUtil.count(iterator());
        }
        return count;
      }

      @Override
      public Iterator<E> iterator() {
        return new FilterIterator<>(G.iterator(), fmember);
      }

      @Override
      public void removeNode(E n) {
        throw new UnsupportedOperationException();
      }
    };

    final EdgeManager<E> edgeManager = new EdgeManager<E>() {

      private Map<E, Collection<E>> succs = new HashMap<>();

      private Map<E, Collection<E>> preds = new HashMap<>();

      private Set<E> getConnected(E inst, Function<E, Iterator<? extends E>> fconnected) {
        Set<E> result = new LinkedHashSet<>();
        Set<E> seenInsts = new HashSet<>();
        Set<E> newInsts = Iterator2Collection.toSet(fconnected.apply(inst));
        while (!newInsts.isEmpty()) {
          Set<E> nextInsts = new HashSet<>();
          for (E s : newInsts) {
            if (!seenInsts.contains(s)) {
              seenInsts.add(s);
              if (nodeManager.containsNode(s)) {
                result.add(s);
              } else {
                Iterator<? extends E> ss = fconnected.apply(s);
                while (ss.hasNext()) {
                  E n = ss.next();
                  if (!seenInsts.contains(n)) {
                    nextInsts.add(n);
                  }
                }
              }
            }
          }
          newInsts = nextInsts;
        }
        return result;
      }

      private void setPredNodes(E N) {
        preds.put(N, getConnected(N, G::getPredNodes));
      }

      private void setSuccNodes(E N) {
        succs.put(N, getConnected(N, G::getSuccNodes));
      }

      @Override
      public int getPredNodeCount(E N) {
        if (!preds.containsKey(N)) {
          setPredNodes(N);
        }
        return preds.get(N).size();
      }

      @Override
      public Iterator<E> getPredNodes(E N) {
        if (!preds.containsKey(N)) {
          setPredNodes(N);
        }
        return preds.get(N).iterator();
      }

      @Override
      public int getSuccNodeCount(E N) {
        if (!succs.containsKey(N)) {
          setSuccNodes(N);
        }
        return succs.get(N).size();
      }

      @Override
      public Iterator<E> getSuccNodes(E N) {
        if (!succs.containsKey(N)) {
          setSuccNodes(N);
        }
        return succs.get(N).iterator();
      }

      @Override
      public boolean hasEdge(E src, E dst) {
        if (!preds.containsKey(dst)) {
          setPredNodes(dst);
        }
        return preds.get(dst).contains(src);
      }

      @Override
      public void addEdge(E src, E dst) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void removeAllIncidentEdges(E node) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
      }

      @Override
      public void removeEdge(E src, E dst) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
      }

      @Override
      public void removeIncomingEdges(E node) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
      }

      @Override
      public void removeOutgoingEdges(E node) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
      }
    };

    return new AbstractGraph<E>() {

      @Override
      protected EdgeManager<E> getEdgeManager() {
        return edgeManager;
      }

      @Override
      protected NodeManager<E> getNodeManager() {
        return nodeManager;
      }

    };

  }

}
