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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.graph.Graph;

/**
 * This class searches depth-first search for node that matches some criteria. If found, it reports a path to the first node found.
 * 
 * This class follows the outNodes of the graph nodes to define the graph, but this behavior can be changed by overriding the
 * getConnected method.
 */
public class DFSPathFinder<T> extends ArrayList<T> {
  public static final long serialVersionUID = 9939900773328288L;

  /**
   * The graph to search
   */
  protected final Graph<T> G;

  /**
   * The Filter which defines the target set of nodes to find
   */
  final private Predicate<T> filter;

  /**
   * an enumeration of all nodes to search from
   */
  final private Iterator<T> roots;

  /**
   * An iterator of child nodes for each node being searched
   */
  final protected Map<Object, Iterator<? extends T>> pendingChildren = HashMapFactory.make(25);

  /**
   * Flag recording whether initialization has happened.
   */
  private boolean initialized = false;

  /**
   * Construct a depth-first enumerator starting with a particular node in a directed graph.
   * 
   * @param G the graph whose nodes to enumerate
   * @throws IllegalArgumentException if G is null
   */
  public DFSPathFinder(Graph<T> G, T N, Predicate<T> f) throws IllegalArgumentException {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    if (!G.containsNode(N)) {
      throw new IllegalArgumentException("source node not in graph: " + N);
    }
    this.G = G;
    this.roots = new NonNullSingletonIterator<>(N);
    this.filter = f;
  }

  /**
   * Construct a depth-first enumerator across the (possibly improper) subset of nodes reachable from the nodes in the given
   * enumeration.
   * 
   * @param nodes the set of nodes from which to start searching
   */
  public DFSPathFinder(Graph<T> G, Iterator<T> nodes, Predicate<T> f) {
    this.G = G;
    this.roots = nodes;
    this.filter = f;
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    if (roots == null) {
      throw new IllegalArgumentException("roots is null");
    }
    if (filter == null) {
      throw new IllegalArgumentException("filter is null");
    }
  }

  private void init() {
    initialized = true;
    if (roots.hasNext()) {
      T n = roots.next();
      push(n);
      setPendingChildren(n, getConnected(n));
    }
  }

  /**
   * @return a List of nodes that specifies the first path found from a root to a node accepted by the filter. Returns null if no
   *         path found.
   */
  public List<T> find() {
    if (!initialized) {
      init();
    }
    while (hasNext()) {
      T n = peek();
      if (filter.test(n)) {
        List<T> path = currentPath();
        advance();
        return path;
      }
      advance();
    }
    return null;
  }

  protected List<T> currentPath() {
    ArrayList<T> result = new ArrayList<>();
    for (T t : this) {
      result.add(0, t);
    }
    return result;
  }

  /**
   * Return whether there are any more nodes left to enumerate.
   * 
   * @return true if there nodes left to enumerate.
   */
  public boolean hasNext() {
    return (!empty());
  }

  /**
   * Method getPendingChildren.
   * 
   * @return Object
   */
  protected Iterator<? extends T> getPendingChildren(T n) {
    return pendingChildren.get(n);
  }

  /**
   * Method setPendingChildren.
   * 
   * @param v
   * @param iterator
   */
  protected void setPendingChildren(T v, Iterator<? extends T> iterator) {
    pendingChildren.put(v, iterator);
  }

  /**
   * Advance to the next graph node in discover time order.
   */
  private void advance() {

    // we always return the top node on the stack.
    T currentNode = peek();

    // compute the next node to return.
    assert getPendingChildren(currentNode) != null;
    do {
      T stackTop = peek();
      for (T child : Iterator2Iterable.make(getPendingChildren(stackTop))) {
        if (getPendingChildren(child) == null) {
          // found a new child.
          push(child);
          setPendingChildren(child, getConnected(child));
          return;
        }
      }
      // didn't find any new children. pop the stack and try again.
      pop();

    } while (!empty());

    // search for the next unvisited root.
    while (roots.hasNext()) {
      T nextRoot = roots.next();
      if (getPendingChildren(nextRoot) == null) {
        push(nextRoot);
        setPendingChildren(nextRoot, getConnected(nextRoot));
      }
    }

    return;
  }

  /**
   * get the out edges of a given node
   * 
   * @param n the node of which to get the out edges
   * @return the out edges
   * 
   */
  protected Iterator<? extends T> getConnected(T n) {
    return G.getSuccNodes(n);
  }
  
  private boolean empty() {
    return size() == 0;
  }

  private void push(T elt) {
    add(elt);
  }
  
  private T peek() {
    return get(size()-1); 
  }
  
  private T pop() {
    T e = get(size()-1);
    remove(size()-1);
    return e;
  }

}
