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
import java.util.Stack;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.Graph;

/**
 * This class implements depth-first search over a Graph, return an enumeration
 * of the nodes of the graph in order of increasing finishing time. This class
 * follows the outNodes of the graph nodes to define the graph, but this
 * behavior can be changed by overriding the getConnected method.
 * 
 * @author Julian Dolby
 * @author Stephen Fink
 */
public abstract class DFSFinishTimeIterator<T> extends Stack<T> implements Iterator<T> {

  /**
   * the current next element in finishing time order
   */
  private T theNextElement;

  /**
   * an enumeration of all nodes to search from
   */
  private Iterator<? extends T> roots;

  /**
   * The governing graph.
   */
  private Graph<T> G;

  /**
   * Subclasses must call this in the construtor!
   * 
   * @param G
   * @param nodes
   */
  protected void init(Graph<T> G, Iterator<? extends T> nodes) {
    this.G = G;
    roots = nodes;
    if (roots.hasNext())
      theNextElement = roots.next();
  }

  /**
   * Return whether there are any more nodes left to enumerate.
   * 
   * @return true if there nodes left to enumerate.
   */
  public boolean hasNext() {
    return (!empty() || (theNextElement != null && getPendingChildren(theNextElement) == null));
  }

  /**
   * Method getPendingChildren.
   * 
   * @return Object
   */
  abstract Iterator getPendingChildren(T n);

  /**
   * Method setPendingChildren.
   * 
   * @param v
   * @param iterator
   */
  abstract void setPendingChildren(T v, Iterator<? extends T> iterator);

  /**
   * Find the next graph node in finishing time order.
   * 
   * @return the next graph node in finishing time order.
   */
  @SuppressWarnings("unchecked")
  public T next() {
    if (empty()) {
      T v = theNextElement;
      setPendingChildren(v, getConnected(v));
      push(v);
    }
    recurse: while (!empty()) {
      T v = peek();
      Iterator<? extends T> pc = getPendingChildren(v);
      for (Iterator<? extends T> e = pc; e.hasNext();) {
        T n = e.next();
        if (Assertions.verifyAssertions) {
          Assertions._assert(n != null, "null node in pc");
        }
        Iterator nChildren = getPendingChildren(n);
        if (nChildren == null) {
          // found a new child: recurse to it.
          setPendingChildren(n, getConnected(n));
          push(n);
          continue recurse;
        }
      }
      // the following saves space by allowing the original iterator to be GCed
      setPendingChildren(v, (Iterator<T>) EmptyIterator.instance());

      // no more children to visit: finished this vertex
      while (getPendingChildren(theNextElement) != null && roots.hasNext()) {
        theNextElement = roots.next();
      }

      return pop();
    }
    return null;
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

  /**
   * @see java.util.Iterator#remove()
   */
  public void remove() throws UnimplementedError {
    throw new UnimplementedError();
  }

}
