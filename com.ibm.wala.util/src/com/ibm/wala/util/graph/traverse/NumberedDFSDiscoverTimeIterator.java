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

import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * This class implements depth-first search over a NumberedGraph, return an enumeration of the nodes of the graph in order of
 * increasing discover time. This class follows the outNodes of the graph nodes to define the graph, but this behavior can be
 * changed by overriding the getConnected method.
 */
public class NumberedDFSDiscoverTimeIterator<T> extends GraphDFSDiscoverTimeIterator<T> {

  private static final long serialVersionUID = -3919708273323217304L;

  /**
   * An iterator of child nodes for each node being searched
   */
  private final Iterator<? extends T>[] pendingChildren;

  /**
   * The Graph being traversed
   */
  protected final NumberedGraph<T> G;

  /**
   * Construct a depth-first enumerator starting with a particular node in a directed graph.
   * 
   * @param G the graph whose nodes to enumerate
   * @throws IllegalArgumentException if G is null
   */
  @SuppressWarnings("unchecked")
  public NumberedDFSDiscoverTimeIterator(NumberedGraph<T> G, T N) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    this.G = G;
    pendingChildren = new Iterator[G.getMaxNumber() + 1];
    init(G, new NonNullSingletonIterator<>(N));
  }

  /**
   * Construct a depth-first enumerator across the (possibly improper) subset of nodes reachable from the nodes in the given
   * enumeration.
   * 
   * @param G the graph whose nodes to enumerate
   * @param nodes the set of nodes from which to start searching
   * @throws IllegalArgumentException if G is null
   * @throws IllegalArgumentException if nodes == null
   */
  @SuppressWarnings("unchecked")
  public NumberedDFSDiscoverTimeIterator(NumberedGraph<T> G, Iterator<? extends T> nodes) throws IllegalArgumentException {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    if (nodes == null) {
      throw new IllegalArgumentException("nodes == null");
    }

    this.G = G;
    pendingChildren = new Iterator[G.getMaxNumber() + 1];
    init(G, nodes);
  }

  /**
   * Constructor DFSFinishTimeIterator.
   * 
   * @param G
   * @throws NullPointerException if G is null
   */
  public NumberedDFSDiscoverTimeIterator(NumberedGraph<T> G) throws NullPointerException {
    this(G, G == null ? null : G.iterator());
  }

  /**
   * Method getPendingChildren.
   * 
   * @return Object
   */
  @Override
  protected Iterator<? extends T> getPendingChildren(T n) {
    return pendingChildren[G.getNumber(n)];
  }

  /**
   * Method setPendingChildren.
   * 
   * @param v
   * @param iterator
   */
  @Override
  protected void setPendingChildren(T v, Iterator<? extends T> iterator) {
    pendingChildren[G.getNumber(v)] = iterator;
  }
}
