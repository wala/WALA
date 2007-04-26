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
package com.ibm.wala.util.graph.traverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Queue;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.Graph;

/**
 * This class searches breadth-first for node that matches some criteria. If
 * found, it reports a path to the first node found.
 * 
 * This class follows the outNodes of the graph nodes to define the graph, but
 * this behavior can be changed by overriding the getConnected method.
 * 
 * TODO: if finding many paths, use a dynamic programming algorithm instead of
 * calling this repeatedly.
 * 
 * @author Stephen Fink
 */
public class BFSPathFinder<T> {

  private final boolean DEBUG = false;

  /**
   * The graph to search
   */
  private Graph<T> G;

  /**
   * The Filter which defines the target set of nodes to find
   */
  private Filter filter;

  /**
   * an enumeration of all nodes to search from
   */
  private Iterator<T> roots;

  /**
   * Construct a breadth-first enumerator starting with a particular node in a
   * directed graph.
   * 
   * @param G
   *          the graph whose nodes to enumerate
   */
  public BFSPathFinder(Graph<T> G, T N, Filter f) {
    this.G = G;
    this.roots = new NonNullSingletonIterator<T>(N);
    this.filter = f;
  }

  /**
   * Construct a breadth-first enumerator starting with a particular node in a
   * directed graph.
   * 
   * @param G
   *          the graph whose nodes to enumerate
   */
  public BFSPathFinder(Graph<T> G, T src, final T target) throws IllegalArgumentException {
    this.G = G;
    this.roots = new NonNullSingletonIterator<T>(src);
    if (!G.containsNode(src)) {
      throw new IllegalArgumentException("src is not in graph " + src);
    }
    this.filter = new Filter() {
      public boolean accepts(Object o) {
        return target.equals(o);
      }
    };
  }

  /**
   * Construct a breadth-first enumerator starting with a particular node in a
   * directed graph.
   * 
   * @param G
   *          the graph whose nodes to enumerate
   */
  public BFSPathFinder(Graph<T> G, T src, Iterator<T> targets) {
    final Set<T> ts = HashSetFactory.make();
    while (targets.hasNext()) {
      ts.add(targets.next());
    }

    this.G = G;
    this.roots = new NonNullSingletonIterator<T>(src);

    this.filter = new Filter() {
      public boolean accepts(Object o) {
        return ts.contains(o);
      }
    };
  }

  /**
   * Construct a breadth-first enumerator starting with any of a set of nodes in
   * a directed graph.
   * 
   * @param G
   *          the graph whose nodes to enumerate
   */
  public BFSPathFinder(Graph<T> G, Iterator<T> sources, final T target) {
    this.G = G;
    this.roots = sources;
    this.filter = new Filter() {
      public boolean accepts(Object o) {
        return target.equals(o);
      }
    };
  }

  /**
   * Construct a breadth-first enumerator across the (possibly improper) subset
   * of nodes reachable from the nodes in the given enumeration.
   * 
   * @param nodes
   *          the set of nodes from which to start searching
   */
  public BFSPathFinder(Graph<T> G, Iterator<T> nodes, Filter f) {
    this.G = G;
    this.roots = nodes;
    this.filter = f;
  }

  /**
   * @return a List of nodes that specifies the first path found from a root to
   *         a node accepted by the filter. Returns null if no path found.
   */
  public List<T> find() {

    Queue<T> Q = new Queue<T>();
    HashMap<Object, T> history = HashMapFactory.make();
    while (roots.hasNext()) {
      history.put(Q.enqueue(roots.next()), null);
    }
    while (!Q.isEmpty()) {
      T N = Q.dequeue();
      if (DEBUG) {
        Trace.println("visit " + N);
      }
      if (filter.accepts(N)) {
        return makePath(N, history);
      }
      Iterator<? extends T> children = getConnected(N);
      while (children.hasNext()) {
        T c = children.next();
        if (!history.containsKey(c)) {
          Q.enqueue(c);
          history.put(c, N);
        }
      }
    }

    return null;
  }

  /**
   * @return a List which represents a path in the breadth-first search to Q[i].
   *         Q holds the nodes visited during the BFS, in order.
   */
  private List<T> makePath(T node, Map<Object, T> history) {
    ArrayList<T> result = new ArrayList<T>();
    T n = node;
    result.add(n);
    while (true) {
      T parent = history.get(n);
      if (parent == null)
        return result;
      else {
        result.add(parent);
        n = parent;
      }
    }
  }

  /**
   * get the out edges of a given node
   * 
   * @param n
   *          the node of which to get the out edges
   * @return the out edges
   * 
   */
  protected Iterator<? extends T> getConnected(T n) {
    return G.getSuccNodes(n);
  }
}
