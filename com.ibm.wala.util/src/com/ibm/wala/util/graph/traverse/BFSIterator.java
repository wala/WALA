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
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.Graph;

/**
 * This class implements breadth-first search over a Graph, returning an Iterator of the nodes of the graph in order of discovery.
 * This class follows the outNodes of the graph nodes to define the graph, but this behavior can be changed by overriding the
 * getConnected method.
 */
public class BFSIterator<T> implements Iterator<T> {

  /**
   * List of nodes as discovered
   */
  final ArrayList<T> Q = new ArrayList<>();

  /**
   * Set of nodes that have been visited
   */
  final HashSet<T> visited = HashSetFactory.make();

  /**
   * index of the node currently being searched
   */
  private int index = 0;

  /**
   * Governing Graph
   */
  protected Graph<T> G;

  /**
   * Construct a breadth-first iterator starting with a particular node in a directed graph.
   * 
   * @param G the graph whose nodes to enumerate
   * @throws IllegalArgumentException if G is null
   */
  public BFSIterator(Graph<T> G, T N) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    init(G, new NonNullSingletonIterator<>(N));
  }

  /**
   * Construct a breadth-first enumerator across the (possibly improper) subset of nodes reachable from the nodes in the given
   * enumeration.
   * 
   * @param nodes the set of nodes from which to start searching
   * @throws IllegalArgumentException if G is null
   */
  public BFSIterator(Graph<T> G, Iterator<? extends T> nodes) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    if (nodes == null) {
      throw new IllegalArgumentException("nodes is null");
    }
    init(G, nodes);
  }

  /**
   * Constructor DFSFinishTimeIterator.
   * 
   * @param G
   * @throws NullPointerException if G is null
   */
  public BFSIterator(Graph<T> G) throws NullPointerException {
    this(G, G == null ? null : G.iterator());
  }

  private void init(Graph<T> G, Iterator<? extends T> nodes) {
    this.G = G;

    while (nodes.hasNext()) {
      T o = nodes.next();
      if (!visited.contains(o)) {
        Q.add(o);
        visited.add(o);
      }
    }
    index = 0;
    if (Q.size() > 0) {
      T current = Q.get(0);
      visitChildren(current);
    }
  }

  private void visitChildren(T N) {
    for (T child : Iterator2Iterable.make(getConnected(N))) {
      if (!visited.contains(child)) {
        Q.add(child);
        visited.add(child);
      }
    }
  }

  /**
   * Return whether there are any more nodes left to enumerate.
   * 
   * @return true if there nodes left to enumerate.
   */
  @Override
  public boolean hasNext() {
    return (Q.size() > index);
  }

  /**
   * Find the next graph node in discover time order.
   * 
   * @return the next graph node in discover time order.
   */
  @Override
  public T next() throws NoSuchElementException {
    if (index >= Q.size()) {
      throw new NoSuchElementException();
    }
    T result = Q.get(index);
    index++;
    if (hasNext()) {
      T N = Q.get(index);
      visitChildren(N);
    }
    return result;
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

  /**
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() throws UnimplementedError {
    throw new UnimplementedError();
  }
}
