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
public class NumberedDFSFinishTimeIterator<T> extends DFSFinishTimeIterator<T> {
  public static final long serialVersionUID = 8737376661L;

  /**
   * An iterator of child nodes for each node being searched
   */
  private Iterator<T>[] pendingChildren;

  /**
   * The Graph being traversed
   */
  private final NumberedGraph<T> G;

  /**
   * Construct a depth-first enumerator starting with a particular node in a directed graph.
   * 
   * @param G the graph whose nodes to enumerate
   */
  @SuppressWarnings("unchecked")
  NumberedDFSFinishTimeIterator(NumberedGraph<T> G, T N) {
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
   */
  @SuppressWarnings("unchecked")
  NumberedDFSFinishTimeIterator(NumberedGraph<T> G, Iterator<? extends T> nodes) {
    this.G = G;
    pendingChildren = new Iterator[G.getMaxNumber() + 1];
    init(G, nodes);
  }

  /**
   * Constructor DFSFinishTimeIterator.
   * 
   * @param G
   */
  NumberedDFSFinishTimeIterator(NumberedGraph<T> G) {
    this(G, G.iterator());
  }

  @SuppressWarnings("unchecked")
  @Override
  Iterator<T> getPendingChildren(T n) {
    int number = G.getNumber(n);
    if (number >= pendingChildren.length) {
      // the graph is probably growing as we travserse
      Iterator<T>[] old = pendingChildren;
      pendingChildren = new Iterator[number * 2];
      System.arraycopy(old, 0, pendingChildren, 0, old.length);
      return null;
    }
    if (number < 0) {
      assert false : "negative number for " + n + " " + n.getClass();
    }
    return pendingChildren[number];
  }

  /**
   * Method setPendingChildren.
   * 
   * @param v
   * @param iterator
   */
  @Override
  void setPendingChildren(T v, Iterator<T> iterator) {
    pendingChildren[G.getNumber(v)] = iterator;
  }
}
