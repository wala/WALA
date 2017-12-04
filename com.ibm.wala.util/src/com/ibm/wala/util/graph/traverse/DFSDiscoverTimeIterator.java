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
import java.util.NoSuchElementException;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * This class implements depth-first search over a {@link NumberedGraph}, return an enumeration of the nodes of the graph in order of
 * increasing discover time. This class follows the outNodes of the graph nodes to define the graph, but this behavior can be
 * changed by overriding the getConnected method.
 */
public abstract class DFSDiscoverTimeIterator<T> extends ArrayList<T> implements Iterator<T> {

  private static final long serialVersionUID = 4238700455408861924L;
  /**
   * an enumeration of all nodes to search from
   */
  private Iterator<? extends T> roots;

  /**
   * subclass constructors must call this!
   */
  protected void init(Iterator<? extends T> nodes) {
    roots = nodes;
    assert nodes != null;
    if (roots.hasNext()) {
      T n = roots.next();
      push(n);
      setPendingChildren(n, getConnected(n));
    }
  }

  /**
   * subclass constructors must call this!
   */
  protected void init(T N) {
    init(new NonNullSingletonIterator<>(N));
  }

  /**
   * Return whether there are any more nodes left to enumerate.
   * 
   * @return true if there nodes left to enumerate.
   */
  @Override
  public boolean hasNext() {
    return (!empty());
  }

  abstract protected Iterator<? extends T> getPendingChildren(T n);

  abstract protected void setPendingChildren(T v, Iterator<? extends T> iterator);

  /**
   * Find the next graph node in discover time order.
   * 
   * @return the next graph node in discover time order.
   */
  @Override
  public T next() throws NoSuchElementException {

    if (empty()) {
      throw new NoSuchElementException();
    }

    // we always return the top node on the stack.
    T toReturn = peek();

    // compute the next node to return.
    assert getPendingChildren(toReturn) != null;
    do {
      T stackTop = peek();
      for (T child : Iterator2Iterable.make(getPendingChildren(stackTop))) {
        if (getPendingChildren(child) == null) {
          // found a new child.
          visitEdge(stackTop, child);
          setPendingChildren(child, getConnected(child));
          push(child);
          return toReturn;
        }
      }
      // the following saves space by allowing the original iterator to be GCed
      Iterator<T> empty = EmptyIterator.instance();
      setPendingChildren(stackTop, empty);
      // didn't find any new children. pop the stack and try again.
      pop();

    } while (!empty());

    // search for the next unvisited root.
    while (roots.hasNext()) {
      T nextRoot = roots.next();
      if (getPendingChildren(nextRoot) == null) {
        push(nextRoot);
        setPendingChildren(nextRoot, getConnected(nextRoot));
        return toReturn;
      }
    }

    return toReturn;
  }

  /**
   * get the out edges of a given node
   * 
   * @param n the node of which to get the out edges
   * @return the out edges
   * 
   */
  abstract protected Iterator<? extends T> getConnected(T n);

  @Override
  public void remove() throws UnimplementedError {
    throw new UnimplementedError();
  }

  /**
   * @param from source of the edge to visit
   * @param to target of the edge to visit
   */
  protected void visitEdge(T from, T to) {
    // do nothing. subclasses will override.
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
