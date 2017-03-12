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

import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.graph.Graph;

/**
 * This class implements depth-first search over a Graph, return an enumeration of the nodes of the graph in order of increasing
 * finishing time. This class follows the outNodes of the graph nodes to define the graph, but this behavior can be changed by
 * overriding the getConnected method.
 */
public class SlowDFSFinishTimeIterator<T> extends DFSFinishTimeIterator<T> {
  public static final long serialVersionUID = 3903190104743762628L;

  /**
   * An iterator of child nodes for each node being searched
   */
  final private Map<T, Iterator<T>> pendingChildren = HashMapFactory.make(25);

  /**
   * Construct a depth-first enumerator starting with a particular node in a directed graph.
   * 
   * @param G the graph whose nodes to enumerate
   * @throws IllegalArgumentException if G is null
   */
  public SlowDFSFinishTimeIterator(Graph<T> G, T N) throws IllegalArgumentException {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    if (!G.containsNode(N)) {
      throw new IllegalArgumentException("source node not in graph: " + N);
    }
    init(G, new NonNullSingletonIterator<>(N));
  }

  /**
   * Construct a depth-first enumerator across the (possibly improper) subset of nodes reachable from the nodes in the given
   * enumeration.
   * 
   * @param G the graph whose nodes to enumerate
   * @param nodes the set of nodes from which to start searching
   */
  public SlowDFSFinishTimeIterator(Graph<T> G, Iterator<? extends T> nodes) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    if (nodes == null) {
      throw new IllegalArgumentException("G is null");
    }
    init(G, nodes);
  }

  /**
   * @throws NullPointerException if G is null
   */
  public SlowDFSFinishTimeIterator(Graph<T> G) throws NullPointerException {
    this(G, G == null ? null : G.iterator());
  }

  @Override
  Iterator<T> getPendingChildren(T n) {
    return pendingChildren.get(n);
  }

  @Override
  void setPendingChildren(T v, Iterator<T> iterator) {
    pendingChildren.put(v, iterator);
  }

}
